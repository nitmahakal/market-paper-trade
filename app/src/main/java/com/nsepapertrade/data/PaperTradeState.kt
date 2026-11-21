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
import java.util.Calendar
import java.util.TimeZone

class PaperTradeState(
    private val engine: PaperTradeEngine,
    private val provider: MarketDataProvider =
        YahooMarketDataProvider()
) : RememberObserver {

    companion object {

        // Market hours: refresh often (even if feed is delayed)
        private const val LIVE_INTERVAL_MS = 10_000L

        // Market hours but no fresh quote for a long time
        // (holiday / stuck feed)
        private const val QUIET_INTERVAL_MS = 60_000L

        // Weekday before open / after close
        private const val AFTER_HOURS_INTERVAL_MS = 5 * 60_000L

        // Saturday / Sunday
        private const val WEEKEND_INTERVAL_MS = 15 * 60_000L

        // IST market session
        private const val OPEN_MS = (9 * 60 + 15) * 60_000L
        private const val CLOSE_MS = (15 * 60 + 30) * 60_000L

        // Feed lag above this is not treated as a live-but-delayed feed
        private const val MAX_FEED_DELAY_MS = 20 * 60_000L

        // Until measured, assume the feed may lag by this much
        private const val DEFAULT_FEED_DELAY_MS = 15 * 60_000L
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

    @Volatile
    private var feedDelayMs = DEFAULT_FEED_DELAY_MS

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

                delay(nextDelayMs(freshestLagMs))
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

        val symbols = positions
            .filter { it.quantity > 0 }
            .map { it.symbol }
            .distinct()

        if (symbols.isEmpty()) {
            return null
        }

        val prices = LinkedHashMap<String, Double>()
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
                    prices[symbol] = quote.price

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

        return freshestLagMs
    }

    // Learn how delayed the feed is, only while the market is open.
    private fun updateFeedDelay(freshestLagMs: Long?) {

        if (freshestLagMs == null ||
            freshestLagMs > MAX_FEED_DELAY_MS
        ) {
            return
        }

        val ist = istNow()

        if (!ist.isWeekend &&
            ist.msOfDay in OPEN_MS until CLOSE_MS
        ) {
            feedDelayMs = freshestLagMs
        }
    }

    private fun nextDelayMs(freshestLagMs: Long?): Long {

        val ist = istNow()

        if (ist.isWeekend) {
            return WEEKEND_INTERVAL_MS
        }

        val now = ist.msOfDay

        // Before open: sleep until just before the open
        if (now < OPEN_MS) {
            return (OPEN_MS - now).coerceIn(
                LIVE_INTERVAL_MS,
                AFTER_HOURS_INTERVAL_MS
            )
        }

        // Market hours + the feed's own delay after close
        val windowEnd = CLOSE_MS + feedDelayMs + 60_000L

        if (now < windowEnd) {

            val feedIsFresh =
                freshestLagMs != null &&
                    freshestLagMs <= MAX_FEED_DELAY_MS

            val justOpened =
                now - OPEN_MS < MAX_FEED_DELAY_MS

            return if (feedIsFresh || justOpened) {
                LIVE_INTERVAL_MS
            } else {
                QUIET_INTERVAL_MS
            }
        }

        return AFTER_HOURS_INTERVAL_MS
    }

    private class IstTime(
        val isWeekend: Boolean,
        val msOfDay: Long
    )

    private fun istNow(): IstTime {

        val calendar = Calendar.getInstance(
            TimeZone.getTimeZone("Asia/Kolkata")
        )

        val day = calendar.get(Calendar.DAY_OF_WEEK)

        val msOfDay = (
            calendar.get(Calendar.HOUR_OF_DAY) * 3_600_000L +
                calendar.get(Calendar.MINUTE) * 60_000L +
                calendar.get(Calendar.SECOND) * 1_000L
            )

        return IstTime(
            isWeekend =
                day == Calendar.SATURDAY ||
                    day == Calendar.SUNDAY,
            msOfDay = msOfDay
        )
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
