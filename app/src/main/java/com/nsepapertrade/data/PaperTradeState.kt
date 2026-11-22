package com.nsepapertrade.data

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nsepapertrade.model.Instrument
import com.nsepapertrade.model.Position
import com.nsepapertrade.model.Trade
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
import java.util.Locale

class PaperTradeState(
    private val engine: PaperTradeEngine,
    private val provider: MarketDataProvider =
        YahooMarketDataProvider(),
    // When given, orders placed while the market is closed are
    // queued and fill at the market open price.
    private val orderBook: OrderBook? = null
) : RememberObserver {

    var snapshot by mutableStateOf(engine.getPortfolioSnapshot())
        private set

    var positions by mutableStateOf(engine.getPositions())
        private set

    var trades by mutableStateOf(engine.getTrades())
        private set

    var queuedOrders by mutableStateOf(
        orderBook?.getQueuedOrders() ?: emptyList()
    )
        private set

    var message by mutableStateOf("")
        private set

    var selectedInstrument by mutableStateOf<Instrument?>(null)
        private set

    private var refreshScope: CoroutineScope? = null

    @Volatile
    private var feedDelayMs = MarketClock.DEFAULT_FEED_DELAY_MS

    // ---------------------------------------------------------
    // Auto refresh of all open positions' prices
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

                val freshestLagMs = refreshPositionPrices()

                updateFeedDelay(freshestLagMs)

                delay(
                    MarketClock.refreshDelayMs(
                        feedDelayMs = feedDelayMs,
                        freshestLagMs = freshestLagMs
                    )
                )
            }
        }
    }

    private fun stopAutoRefresh() {
        refreshScope?.cancel()
        refreshScope = null
    }

    // Returns how old the freshest quote was (ms),
    // or null if no quote could be fetched.
    private suspend fun refreshPositionPrices(): Long? {

        val symbols = LinkedHashSet<String>()

        positions
            .filter { it.quantity > 0 }
            .forEach { symbols.add(it.symbol) }

        queuedOrders.forEach { symbols.add(it.symbol) }

        if (symbols.isEmpty()) {
            return null
        }

        val quotes = LinkedHashMap<String, MarketQuote>()
        var freshestLagMs: Long? = null

        for (symbol in symbols) {
            try {
                val quote = provider.getQuote(
                    Instrument(
                        symbol = symbol,
                        name = symbol
                    )
                )

                if (quote != null && quote.price > 0.0) {
                    quotes[symbol] = quote

                    val lag = (
                        System.currentTimeMillis() -
                            quote.timestamp
                        ).coerceAtLeast(0L)

                    val current = freshestLagMs

                    freshestLagMs =
                        if (current == null || lag < current) {
                            lag
                        } else {
                            current
                        }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // skip this symbol, try again next round
            }
        }

        if (quotes.isNotEmpty()) {
            // Apply on the main thread, same as buy/sell,
            // so position updates never overlap.
            withContext(Dispatchers.Main) {

                quotes.forEach { (symbol, quote) ->
                    engine.updateMarketPrice(
                        symbol = symbol,
                        price = quote.price
                    )
                }

                if (MarketClock.isOpen()) {
                    fillQueuedOrders(quotes)
                }

                refreshPortfolio()
            }
        }

        return freshestLagMs
    }

    // Learn how delayed the feed is, only while the market is open.
    private fun updateFeedDelay(freshestLagMs: Long?) {

        if (freshestLagMs != null &&
            freshestLagMs <= MarketClock.MAX_FEED_DELAY_MS &&
            MarketClock.isOpen()
        ) {
            feedDelayMs = freshestLagMs
        }
    }

    // Main thread.
    private fun fillQueuedOrders(
        quotes: Map<String, MarketQuote>
    ) {
        val book = orderBook ?: return

        for (order in book.getQueuedOrders()) {

            val quote = quotes[order.symbol] ?: continue

            // Only quotes of today's session: on a weekday
            // holiday the order simply stays queued.
            if (!MarketClock.isFromCurrentSession(
                    quote.timestamp
                )
            ) {
                continue
            }

            val fillPrice =
                if (quote.openPrice > 0.0) {
                    quote.openPrice
                } else {
                    quote.price
                }

            val result =
                if (order.side == TradeSide.BUY) {
                    engine.buy(
                        symbol = order.symbol,
                        quantity = order.quantity,
                        price = fillPrice
                    )
                } else {
                    engine.sell(
                        symbol = order.symbol,
                        quantity = order.quantity,
                        price = fillPrice
                    )
                }

            book.removeQueuedOrder(order.id)

            message =
                if (result.isSuccess) {
                    "Queued ${order.side.name} filled at open: " +
                        "${order.symbol} × ${order.quantity} " +
                        "@ ₹" +
                        "%.2f".format(Locale.US, fillPrice)
                } else {
                    "Queued ${order.side.name} failed: " +
                        "${order.symbol} — " +
                        (result.exceptionOrNull()?.message
                            ?: "error")
                }
        }

        queuedOrders = book.getQueuedOrders()
    }

    // ---------------------------------------------------------
    // Selection and orders
    // ---------------------------------------------------------

    fun selectInstrument(instrument: Instrument) {
        selectedInstrument = instrument
        message = "${instrument.symbol} selected"
    }

    fun clearSelectedInstrument() {
        selectedInstrument = null
        message = ""
    }

    fun buy(symbol: String, quantity: Int, price: Double) {

        if (queueIfMarketClosed(TradeSide.BUY, symbol, quantity)) {
            return
        }

        val result = engine.buy(
            symbol = symbol,
            quantity = quantity,
            price = price
        )

        if (result.isSuccess) {
            refreshPortfolio()
            message = "BUY successful: $symbol × $quantity"
        } else {
            message = result.exceptionOrNull()?.message ?: "BUY failed"
        }
    }

    fun sell(symbol: String, quantity: Int, price: Double) {

        if (queueIfMarketClosed(TradeSide.SELL, symbol, quantity)) {
            return
        }

        val result = engine.sell(
            symbol = symbol,
            quantity = quantity,
            price = price
        )

        if (result.isSuccess) {
            refreshPortfolio()
            message = "SELL successful: $symbol × $quantity"
        } else {
            message = result.exceptionOrNull()?.message ?: "SELL failed"
        }
    }

    fun cancelQueuedOrder(id: Long) {
        val book = orderBook ?: return

        book.removeQueuedOrder(id)
        queuedOrders = book.getQueuedOrders()
        message = "Queued order cancelled"
    }

    // Returns true if the order was queued (market closed).
    private fun queueIfMarketClosed(
        side: TradeSide,
        symbol: String,
        quantity: Int
    ): Boolean {

        val book = orderBook ?: return false

        if (MarketClock.isOpen()) {
            return false
        }

        if (quantity <= 0) {
            message = "Quantity must be greater than zero"
            return true
        }

        book.addQueuedOrder(
            QueuedOrder(
                id = book.nextId(),
                kind = OrderKind.EQUITY,
                symbol = symbol,
                contract = null,
                side = side,
                quantity = quantity,
                createdAt = System.currentTimeMillis()
            )
        )

        queuedOrders = book.getQueuedOrders()

        message =
            "${side.name} queued: $symbol × $quantity. " +
                "Executes at the market open price " +
                "(next open: ${MarketClock.nextOpenLabel()})."

        return true
    }

    private fun refreshPortfolio() {
        snapshot = engine.getPortfolioSnapshot()
        positions = engine.getPositions()
        trades = engine.getTrades()
    }

    fun getPosition(symbol: String): Position? {
        return positions.firstOrNull {
            it.symbol.equals(symbol, ignoreCase = true)
        }
    }

    fun getTradesForSymbol(symbol: String): List<Trade> {
        return trades.filter {
            it.symbol.equals(symbol, ignoreCase = true)
        }
    }
}
