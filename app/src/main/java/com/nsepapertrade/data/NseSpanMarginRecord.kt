package com.nsepapertrade.data

data class NseSpanMarginRecord(
    val contractSymbol: String,
    val underlying: String,
    val expiry: String,
    val strikePrice: Double,
    val optionType: String,
    val contractType: String,
    val lotSize: Int,
    val scanRange: Double,
    val volatilityScanRange: Double,
    val riskArray: List<Double>,
    val timestamp: Long
) {

    val scenarioCount: Int
        get() = riskArray.size
}
