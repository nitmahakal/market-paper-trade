package com.nsepapertrade.data

data class NseSpanMarginRecord(
    val contractSymbol: String,
    val scanRange: Double,
    val scanRiskUp: Double,
    val scanRiskDown: Double,
    val timestamp: Long
)
