package com.nsepapertrade.data

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nsepapertrade.model.Instrument
import com.nsepapertrade.model.Position
import com.nsepapertrade.model.Trade
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PaperTradeState(
    private val engine: PaperTradeEngine,
    private val provider: MarketDataProvider =
        YahooMarketDataProvider()
) : RememberObserver {

    companion object {
        private const val LIVE_INTERVAL_MS = 10_000L
        private const val IDLE_INTERVAL_MS = 60_000L
    }

    var snapshot by mutableStateOf(engine.getPortfolioSnapshot())
        private set

    var positions by mutableStateOf(engine.getPositions())
        private set

    var trades by mutableStateOf(engine.getTrades())
        private set

    var message by mutableStateOf("")
        private set

    var selectedInstrument by mutableStateOf<Instrument?>(null)
        private set

    private var refreshScope: CoroutineScope? = null

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

                val anyLive = refreshPositionPrices()

                delay(
                    if (anyLive) {
                        LIVE_INTERVAL_MS
                    } else {
                        IDLE_INTERVAL_MS
                    }
                )
            }
        }
    }

    private fun stopAutoRefresh() {
        refreshScope?.cancel()
        refreshScope = null
    }

    // Returns true if at least one quote was live.
    private suspend fun refreshPositionPrices(): Boolean {

        val symbols = positions
            .filter { it.quantity > 0 }
            .map { it.symbol }
            .distinct()

        if (symbols.isEmpty()) {
            return false
        }

        val prices = LinkedHashMap<String, Double>()
        var anyLive = false

        for (symbol in symbols) {
            try {
                val quote = provider.getQuote(
                    Instrument(
                        symbol = symbol,
                        name = symbol
                    )
                )

                if (quote != null && quote.price > 0.0) {
                    prices[symbol] = quote.price

                    if (quote.source == MarketDataSource.LIVE) {
                        anyLive = true
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // skip this symbol, try again next round
            }
        }

        if (prices.isNotEmpty()) {
            // Apply on the main thread, same as buy/sell,
            // so position updates never overlap.
            withContext(Dispatchers.Main) {
                prices.forEach { (symbol, price) ->
                    engine.updateMarketPrice(
                        symbol = symbol,
                        price = price
                    )
                }

                refreshPortfolio()
            }
        }

        return anyLive
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
