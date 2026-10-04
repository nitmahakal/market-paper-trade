package com.nsepapertrade.model

data class FnoTrade(
    val id: Long,
    val contract: FnoContract,
    val side: TradeSide,
    val positionSide: FnoPositionSide,
    val lots: Int,
    val price: Double,
    val charge: Double,
    val timestamp: Long
) {

    val quantity: Int
        get() = lots * contract.lotSize
}
