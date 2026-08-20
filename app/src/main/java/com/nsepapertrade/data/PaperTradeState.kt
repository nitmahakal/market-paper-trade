package com.nsepapertrade.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nsepapertrade.model.Instrument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PaperTradeState(
    private val engine: PaperTradeEngine,
    private val marketDataRepository: MarketDataRepository,
    private val scope: CoroutineScope
) {

    var snapshot by mutableStateOf(
        engine.getPortfolioSnapshot()
    )
        private set

    var message by mutableStateOf("")
        private set

    var selectedInstrument by mutableStateOf<Instrument?>(null)
        private set

    var quote by mutableStateOf<MarketQuote?>(null)
        private set

    var marketDataError by mutableStateOf("")
        private set

    private var quoteJob: Job? = null

    fun selectInstrument(instrument: Instrument) {
        selectedInstrument = instrument
        quote = null
        marketDataError = ""
        message = "${instrument.symbol} selected"

        loadQuote(instrument)
    }

    fun loadQuote(instrument: Instrument) {
        quoteJob?.cancel()

        quoteJob = scope.launch {
            try {
                val result = marketDataRepository.getQuote(instrument)

                quote = result

                marketDataError = if (result == null) {
                    "No market data available."
                } else {
                    ""
                }
            } catch (e: Exception) {
                quote = null
                marketDataError = e.message ?: "Market data error."
            }
        }
    }

    fun buy(
        symbol: String,
        quantity: Int,
        price: Double
    ) {
        val result = engine.buy(
            symbol = symbol,
            quantity = quantity,
            price = price
        )

        if (result.isSuccess) {
            snapshot = engine.getPortfolioSnapshot()
            message = "BUY successful: $symbol × $quantity"
        } else {
            message = result.exceptionOrNull()?.message ?: "BUY failed"
        }
    }

    fun sell(
        symbol: String,
        quantity: Int,
        price: Double
    ) {
        val result = engine.sell(
            symbol = symbol,
            quantity = quantity,
            price = price
        )

        if (result.isSuccess) {
            snapshot = engine.getPortfolioSnapshot()
            message = "SELL successful: $symbol × $quantity"
        } else {
            message = result.exceptionOrNull()?.message ?: "SELL failed"
        }
    }
}
