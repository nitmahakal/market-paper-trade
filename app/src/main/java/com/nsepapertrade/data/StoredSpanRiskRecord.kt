package com.nsepapertrade.data

data class StoredSpanRiskRecord(
    val contractSymbol: String,
    val riskArray: List<Double>,
    val contractValueFactor: Double = 1.0,
    val timestamp: Long = 0L
) {

    val scenarioCount: Int
        get() = riskArray.size
}
