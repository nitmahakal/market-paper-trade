package com.nsepapertrade.data

import com.nsepapertrade.model.FnoPosition
import com.nsepapertrade.model.FnoTrade

class FnoTradeRepository(
    private val persistence: FnoTradePersistence? = null
) {

    private val positions =
        mutableListOf<FnoPosition>()

    private val trades =
        mutableListOf<FnoTrade>()

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

    fun getPositions(): List<FnoPosition> =
        positions.sortedWith(
            compareBy<FnoPosition> {
                it.contract.underlying.uppercase()
            }
                .thenBy {
                    it.contract.expiry
                }
                .thenBy {
                    it.contract.contractType.name
                }
                .thenBy {
                    it.contract.strikePrice
                }
                .thenBy {
                    it.contract.optionType.name
                }
                .thenBy {
                    it.side.name
                }
        )

    fun getTrades(): List<FnoTrade> =
        trades.sortedBy {
            it.timestamp
        }

    fun getPosition(
        contractSymbol: String
    ): FnoPosition? =
        positions.firstOrNull {
            it.contract.symbol.equals(
                contractSymbol,
                ignoreCase = true
            )
        }

    fun getTradesForContract(
        contractSymbol: String
    ): List<FnoTrade> =
        trades
            .filter {
                it.contract.symbol.equals(
                    contractSymbol,
                    ignoreCase = true
                )
            }
            .sortedBy {
                it.timestamp
            }

    fun addPosition(
        position: FnoPosition
    ) {
        positions.removeAll {
            it.contract.symbol.equals(
                position.contract.symbol,
                ignoreCase = true
            ) &&
                it.side == position.side
        }

        positions.add(position)

        persistence?.savePositions(
            positions
        )
    }

    fun removePosition(
        contractSymbol: String,
        side: com.nsepapertrade.model.FnoPositionSide
    ) {
        positions.removeAll {
            it.contract.symbol.equals(
                contractSymbol,
                ignoreCase = true
            ) &&
                it.side == side
        }

        persistence?.savePositions(
            positions
        )
    }

    fun addTrade(
        trade: FnoTrade
    ) {
        trades.add(trade)

        persistence?.saveTrades(
            trades
        )
    }
}
