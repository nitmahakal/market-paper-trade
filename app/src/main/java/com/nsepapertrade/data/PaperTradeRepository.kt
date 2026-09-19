package com.nsepapertrade.data

import com.nsepapertrade.model.Position
import com.nsepapertrade.model.Trade

class PaperTradeRepository(
    private val persistence: PaperTradePersistence? = null
) {

    private val positions = mutableListOf<Position>()
    private val trades = mutableListOf<Trade>()

    init {
        if (persistence != null) {
            positions.addAll(
                persistence.loadPositions()
            )

            trades.addAll(
                persistence.loadTrades()
            )
        }
    }

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
            .filter {
                it.symbol.equals(
                    symbol,
                    ignoreCase = true
                )
            }
            .sortedBy { it.timestamp }
    }

    fun addPosition(position: Position) {
        positions.removeAll {
            it.symbol.equals(
                position.symbol,
                ignoreCase = true
            )
        }

        positions.add(position)

        persistence?.savePositions(
            positions
        )
    }

    fun removePosition(symbol: String) {
        positions.removeAll {
            it.symbol.equals(
                symbol,
                ignoreCase = true
            )
        }

        persistence?.savePositions(
            positions
        )
    }

    fun addTrade(trade: Trade) {
        trades.add(trade)

        persistence?.saveTrades(
            trades
        )
    }
}
