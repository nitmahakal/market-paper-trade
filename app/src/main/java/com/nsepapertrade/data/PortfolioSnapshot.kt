package com.nsepapertrade.data

data class PortfolioSnapshot(
    val availableCash: Double,
    val investedValue: Double,
    val currentValue: Double,
    val unrealizedPnl: Double,
    val totalValue: Double
)
