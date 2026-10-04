package com.nsepapertrade.data

data class FnoMarginSnapshot(
    val futureMargin: Double,
    val optionSellMargin: Double,
    val timestamp: Long
)
