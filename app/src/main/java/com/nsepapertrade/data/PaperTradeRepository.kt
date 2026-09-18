package com.nsepapertrade.data

import com.nsepapertrade.model.Position
import com.nsepapertrade.model.Trade

class PaperTradeRepository {

    private val positions = mutableListOf<Position>()
    private val trades = mutableListOf<Trade>()

    fun getPositions(): List<Position> {
        return positions
            .sortedBy { it.symbol.uppercase() }
    }

    fun getTrades(): List<Trade> {
        return trades
            .sortedWith(
                compareBy<Trade> { it.symbol.uppercase() }
                    .thenBy { it.timestamp }
            )
    }

    fun getTradesForSymbol(symbol: String): List<Trade> {
        return trades
            .filter { it.symbol.equals(symbol, ignoreCase = true) }
            .sortedBy { it.timestamp }
    }

    fun addPosition(position: Position) {
        positions.removeAll {
            it.symbol.equals(position.symbol, ignoreCase = true)
        }
        positions.add(position)
    }

    fun removePosition(symbol: String) {
        positions.removeAll {
            it.symbol.equals(symbol, ignoreCase = true)
        }
    }

    fun addTrade(trade: Trade) {
        trades.add(trade)
    }
}
