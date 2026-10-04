package com.nsepapertrade.data

data class FnoContractMargin(
    val contractSymbol: String,
    val futuresMargin: Double = 0.0,
    val optionSellMargin: Double = 0.0,
    val timestamp: Long = 0L
)
