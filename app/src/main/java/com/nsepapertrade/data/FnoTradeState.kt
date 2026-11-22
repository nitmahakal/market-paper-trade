package com.nsepapertrade.data

import android.content.Context
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.FnoPortfolioSnapshot
import com.nsepapertrade.model.FnoPosition
import com.nsepapertrade.model.FnoPositionSide
import com.nsepapertrade.model.Instrument
import com.nsepapertrade.model.TradeSide
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FnoOrderResult(
    val success: Boolean,
    val message: String
)

// All F&O trading state in one place: positions, queued orders,
// target / stop-loss orders, and the auto refresh that drives them.
// Orders and trades are only touched on the main thread.
class FnoTradeState(
    context: Context,
    private val provider: FnoMarketDataProvider =
        NseFnoMarketDataProvider(),
    private val sessionProbe: MarketDataProvider =
        YahooMarketDataProvider()
) : RememberObserver {

    private val appContext = context.applicationContext

    private val engine = FnoTradeEngine(context)

    private val book = OrderBook(context, OrderKind.FNO)

    var snapshot by mutableStateOf(engine.getPortfolioSnapshot())
        private set

    var positions by mutableStateOf(engine.getPositions())
        private set

    var queuedOrders by mutableStateOf(book.getQueuedOrders())
        private set

    var exitOrders by mutableStateOf(book.getExitOrders())
        private set

    // Newest first: fills, triggers, failures.
    var activity by mutableStateOf<List<String>>(emptyList())
        private set

    // Last market data problem (blocked, no internet...). Empty if fine.
    var dataError by mutableStateOf("")
        private set

    private var refreshScope: CoroutineScope? = null

    @Volatile
    private var lotSizesLoaded = false

    // ---------------------------------------------------------
    // Orders from the screen
    // ---------------------------------------------------------

    fun placeOrder(
        contract: FnoContract,
        side: TradeSide,
        lots: Int,
        price: Double
    ): FnoOrderResult {

        if (lots <= 0) {
            return FnoOrderResult(
                false,
                "Lots must be greater than zero."
            )
        }

        if (!contract.isActive) {
            return FnoOrderResult(
                false,
                "This F&O contract is inactive."
            )
        }

        if (!MarketClock.isOpen()) {

            book.addQueuedOrder(
                QueuedOrder(
                    id = book.nextId(),
                    kind = OrderKind.FNO,
                    symbol = contract.symbol,
                    contract = contract,
                    side = side,
                    quantity = lots,
                    createdAt = System.currentTimeMillis()
                )
            )

            refreshState()

            return FnoOrderResult(
                true,
                "${side.name} queued: ${contract.displayName} " +
                    "× $lots lots. Executes at the market open " +
                    "price (next open: ${MarketClock.nextOpenLabel()})."
            )
        }

        if (price <= 0.0) {
            return FnoOrderResult(
                false,
                "Price not available yet."
            )
        }

        val result = execute(contract, side, lots, price)

        refreshState()

        return if (result.isSuccess) {
            FnoOrderResult(true, "${side.name} successful")
        } else {
            FnoOrderResult(
                false,
                result.exceptionOrNull()?.message
                    ?: "${side.name} failed"
            )
        }
    }

    fun addExitOrder(
        contract: FnoContract,
        type: ExitType,
        lots: Int,
        triggerPrice: Double
    ): FnoOrderResult {

        val position = engine.getPositions().firstOrNull {
            it.contract.symbol.equals(
                contract.symbol,
                ignoreCase = true
            )
        } ?: return FnoOrderResult(
            false,
            "No open position in this contract."
        )

        if (lots <= 0 || lots > position.lots) {
            return FnoOrderResult(
                false,
                "Lots must be between 1 and ${position.lots}."
            )
        }

        if (triggerPrice <= 0.0) {
            return FnoOrderResult(
                false,
                "Enter a valid trigger price."
            )
        }

        val mustBeAbove = isAbovePrice(position.side, type)
        val last = position.lastPrice

        if (last > 0.0) {

            val wrongSide =
                if (mustBeAbove) {
                    triggerPrice <= last
                } else {
                    triggerPrice >= last
                }

            if (wrongSide) {
                return FnoOrderResult(
                    false,
                    "For a ${position.side.name} position the " +
                        "${typeLabel(type)} must be " +
                        (if (mustBeAbove) "above" else "below") +
                        " the current price (₹${money(last)})."
                )
            }
        }

        book.addExitOrder(
            ExitOrder(
                id = book.nextId(),
                contract = contract,
                positionSide = position.side,
                type = type,
                lots = lots,
                triggerPrice = triggerPrice,
                createdAt = System.currentTimeMillis()
            )
        )

        refreshState()

        return FnoOrderResult(
            true,
            "${typeLabel(type)} set: $lots lots at " +
                "₹${money(triggerPrice)}"
        )
    }

    fun cancelExitOrder(id: Long) {
        book.removeExitOrder(id)
        refreshState()
    }

    fun cancelQueuedOrder(id: Long) {
        book.removeQueuedOrder(id)
        refreshState()
    }

    // For quotes the screen already fetched for a selected contract.
    fun updateMarketPrice(
        contractSymbol: String,
        price: Double
    ) {
        engine.updateMarketPrice(
            contractSymbol = contractSymbol,
            price = price
        )

        refreshState()
    }

    // ---------------------------------------------------------
    // Auto refresh
    // ---------------------------------------------------------

    override fun onRemembered() {
        startAutoRefresh()
    }

    override fun onForgotten() {
        stopAutoRefresh()
    }

    override fun onAbandoned() {
        stopAutoRefresh()
    }

    private fun startAutoRefresh() {
        if (refreshScope != null) {
            return
        }

        val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

        refreshScope = scope

        scope.launch {
            while (isActive) {

                refreshOnce()

                delay(
                    MarketClock.refreshDelayMs(
                        feedDelayMs = 0L,
                        freshestLagMs = 0L
                    )
                )
            }
        }
    }

    private fun stopAutoRefresh() {
        refreshScope?.cancel()
        refreshScope = null
    }

    // Gives the option chain reader the real lot size of each
    // underlying (it comes from the stored contract list).
    private fun loadLotSizes() {
        try {
            val sizes = HashMap<String, Int>()

            FnoContractStore(appContext)
                .loadContracts()
                .forEach { contract ->
                    if (contract.contractType ==
                        FnoContractType.OPTION &&
                        contract.lotSize > 0
                    ) {
                        sizes[contract.underlying.uppercase()] =
                            contract.lotSize
                    }
                }

            if (sizes.isNotEmpty()) {
                NseFnoMarketDataProvider.setLotSizes(sizes)
                lotSizesLoaded = true
            }
        } catch (e: Exception) {
            // the contract list may still be downloading; retry later
        }
    }

    private suspend fun refreshOnce() {

        if (!lotSizesLoaded) {
            loadLotSizes()
        }

        // Everything that needs a price: open positions,
        // contracts with target / stop-loss, queued orders.
        val contracts = LinkedHashMap<String, FnoContract>()

        positions.forEach {
            contracts[it.contract.symbol] = it.contract
        }

        exitOrders.forEach {
            contracts[it.contract.symbol] = it.contract
        }

        queuedOrders.forEach { order ->
            order.contract?.let {
                contracts[it.symbol] = it
            }
        }

        if (contracts.isEmpty()) {
            if (dataError.isNotEmpty()) {
                dataError = ""
            }
            return
        }

        val quotes = LinkedHashMap<String, MarketQuote>()
        var lastError = ""

        for (contract in contracts.values) {
            try {
                val quote = provider.getQuote(contract)

                if (quote != null && quote.price > 0.0) {
                    quotes[contract.symbol] = quote
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError =
                    e.message ?: "F&O market data error."
            }
        }

        dataError =
            if (quotes.isNotEmpty()) {
                ""
            } else if (lastError.isNotBlank()) {
                lastError
            } else {
                "No market data available."
            }

        // Queued orders may fill only on a real trading day.
        val sessionLive =
            queuedOrders.isNotEmpty() &&
                MarketClock.isOpen() &&
                isTradingSessionLive()

        withContext(Dispatchers.Main) {

            quotes.forEach { (symbol, quote) ->
                engine.updateMarketPrice(
                    contractSymbol = symbol,
                    price = quote.price
                )
            }

            if (MarketClock.isOpen()) {

                if (sessionLive) {
                    fillQueuedOrders(quotes)
                }

                checkExitOrders(quotes)
            }

            refreshState()
        }
    }

    // A weekday holiday has no fresh quotes for a big stock.
    private suspend fun isTradingSessionLive(): Boolean {
        return try {
            val quote = sessionProbe.getQuote(
                Instrument(
                    symbol = "RELIANCE",
                    name = "Reliance Industries"
                )
            )

            quote != null &&
                MarketClock.isFromCurrentSession(quote.timestamp)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    // Main thread.
    private fun fillQueuedOrders(
        quotes: Map<String, MarketQuote>
    ) {
        for (order in book.getQueuedOrders()) {

            val contract = order.contract

            if (contract == null) {
                book.removeQueuedOrder(order.id)
                continue
            }

            val quote = quotes[contract.symbol] ?: continue

            val fillPrice =
                if (quote.openPrice > 0.0) {
                    quote.openPrice
                } else {
                    quote.price
                }

            val result = execute(
                contract = contract,
                side = order.side,
                lots = order.quantity,
                price = fillPrice
            )

            book.removeQueuedOrder(order.id)

            if (result.isSuccess) {
                log(
                    "Queued ${order.side.name} filled at open: " +
                        "${contract.displayName} × " +
                        "${order.quantity} lots @ ₹${money(fillPrice)}"
                )
            } else {
                log(
                    "Queued ${order.side.name} failed: " +
                        "${contract.displayName} — " +
                        (result.exceptionOrNull()?.message
                            ?: "error")
                )
            }
        }
    }

    // Main thread.
    private fun checkExitOrders(
        quotes: Map<String, MarketQuote>
    ) {
        for (order in book.getExitOrders()) {

            val quote =
                quotes[order.contract.symbol] ?: continue

            val price = quote.price

            val position = findPosition(
                order.contract.symbol,
                order.positionSide
            )

            if (position == null) {
                book.removeExitOrder(order.id)
                continue
            }

            if (!isTriggered(order, price)) {
                continue
            }

            val lots = minOf(order.lots, position.lots)

            val result =
                if (order.positionSide == FnoPositionSide.LONG) {
                    engine.sell(order.contract, lots, price)
                } else {
                    engine.buy(order.contract, lots, price)
                }

            book.removeExitOrder(order.id)

            if (result.isSuccess) {
                log(
                    "${typeLabel(order.type)} hit: " +
                        "${order.contract.displayName} — closed " +
                        "$lots lots @ ₹${money(price)}"
                )
            } else {
                log(
                    "${typeLabel(order.type)} failed: " +
                        "${order.contract.displayName} — " +
                        (result.exceptionOrNull()?.message
                            ?: "error")
                )
            }

            // Position fully closed: drop its other exit orders.
            if (findPosition(
                    order.contract.symbol,
                    order.positionSide
                ) == null
            ) {
                book.getExitOrders()
                    .filter {
                        it.contract.symbol.equals(
                            order.contract.symbol,
                            ignoreCase = true
                        )
                    }
                    .forEach { book.removeExitOrder(it.id) }
            }
        }
    }

    private fun findPosition(
        contractSymbol: String,
        side: FnoPositionSide
    ): FnoPosition? {
        return engine.getPositions().firstOrNull {
            it.contract.symbol.equals(
                contractSymbol,
                ignoreCase = true
            ) && it.side == side
        }
    }

    private fun isTriggered(
        order: ExitOrder,
        price: Double
    ): Boolean {
        return if (isAbovePrice(order.positionSide, order.type)) {
            price >= order.triggerPrice
        } else {
            price <= order.triggerPrice
        }
    }

    // True if the trigger sits above the current price:
    // LONG target, SHORT stop-loss.
    private fun isAbovePrice(
        side: FnoPositionSide,
        type: ExitType
    ): Boolean {
        return when (side) {
            FnoPositionSide.LONG -> type == ExitType.TARGET
            FnoPositionSide.SHORT -> type == ExitType.STOP_LOSS
        }
    }

    private fun execute(
        contract: FnoContract,
        side: TradeSide,
        lots: Int,
        price: Double
    ): Result<Unit> {
        return if (side == TradeSide.BUY) {
            engine.buy(contract, lots, price)
        } else {
            engine.sell(contract, lots, price)
        }
    }

    private fun refreshState() {
        snapshot = engine.getPortfolioSnapshot()
        positions = engine.getPositions()
        queuedOrders = book.getQueuedOrders()
        exitOrders = book.getExitOrders()
    }

    private fun log(text: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US)
            .format(Date())

        activity = (listOf("$time  $text") + activity).take(20)
    }

    private fun typeLabel(type: ExitType): String {
        return if (type == ExitType.TARGET) {
            "Target"
        } else {
            "Stop-loss"
        }
    }

    private fun money(value: Double): String {
        return "%.2f".format(Locale.US, value)
    }
}
