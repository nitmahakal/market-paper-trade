package com.nsepapertrade.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nsepapertrade.model.Instrument
import com.nsepapertrade.model.Position
import com.nsepapertrade.model.Trade

class PaperTradeState(
    private val engine: PaperTradeEngine
) {

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
