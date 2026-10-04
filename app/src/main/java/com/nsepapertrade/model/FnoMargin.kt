package com.nsepapertrade.model

enum class FnoMarginType {
    NONE,
    FUTURE,
    OPTION_BUY,
    OPTION_SELL
}

data class FnoMargin(
    val marginType: FnoMarginType,
    val requiredAmount: Double,
    val reservedAmount: Double = requiredAmount
) {

    val availableAmount: Double
        get() = reservedAmount

    fun release(): Double =
        reservedAmount
}
