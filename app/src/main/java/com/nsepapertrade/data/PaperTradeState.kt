package com.nsepapertrade.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nsepapertrade.model.Instrument

class PaperTradeState(
    private val engine: PaperTradeEngine
) {

    var snapshot by mutableStateOf(
        engine.getPortfolioSnapshot()
    )
        private set

    var message by mutableStateOf("")
        private set

    var selectedInstrument by mutableStateOf<Instrument?>(null)
        private set

    fun selectInstrument(instrument: Instrument) {
        selectedInstrument = instrument
        message = "${instrument.symbol} selected"
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
            message = result.exceptionOrNull()?.message
                ?: "BUY failed"
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
            message = result.exceptionOrNull()?.message
                ?: "SELL failed"
        }
    }
}
