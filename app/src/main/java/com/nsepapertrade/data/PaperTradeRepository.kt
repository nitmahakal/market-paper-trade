package com.nsepapertrade.data

import com.nsepapertrade.model.Position
import com.nsepapertrade.model.Trade

class PaperTradeRepository {

    private val positions = mutableListOf<Position>()
    private val trades = mutableListOf<Trade>()

    fun getPositions(): List<Position> {
        return positions.toList()
    }

    fun getTrades(): List<Trade> {
        return trades.toList()
    }

    fun addPosition(position: Position) {
        positions.removeAll { it.symbol == position.symbol }
        positions.add(position)
    }

    fun removePosition(symbol: String) {
        positions.removeAll { it.symbol == symbol }
    }

    fun addTrade(trade: Trade) {
        trades.add(trade)
    }
}
