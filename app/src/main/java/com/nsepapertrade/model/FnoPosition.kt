package com.nsepapertrade.model

enum class FnoPositionSide {
    LONG,
    SHORT
}

data class FnoPosition(
    val contract: FnoContract,
    val side: FnoPositionSide,
    val lots: Int,
    val averagePrice: Double,
    val lastPrice: Double = 0.0,
    val reservedMargin: Double = 0.0
) {

    val quantity: Int
        get() = lots * contract.lotSize

    val investedValue: Double
        get() = quantity * averagePrice

    val currentValue: Double
        get() = quantity * lastPrice

    val unrealizedPnl: Double
        get() {
            val priceDifference =
                when (side) {
                    FnoPositionSide.LONG ->
                        lastPrice - averagePrice

                    FnoPositionSide.SHORT ->
                        averagePrice - lastPrice
                }

            return priceDifference * quantity
        }
}
