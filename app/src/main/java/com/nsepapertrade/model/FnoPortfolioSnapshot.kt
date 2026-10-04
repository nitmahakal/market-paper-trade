package com.nsepapertrade.model

data class FnoPortfolioSnapshot(
    val availableCash: Double,
    val reservedMargin: Double,
    val realizedPnl: Double,
    val unrealizedPnl: Double
) {

    val totalMarginUsed: Double
        get() = reservedMargin

    val totalValue: Double
        get() =
            availableCash +
                reservedMargin +
                realizedPnl +
                unrealizedPnl
}
