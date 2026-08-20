package com.nsepapertrade.model

data class Stock(
    val symbol: String,
    val name: String,
    val instrumentType: InstrumentType = InstrumentType.EQUITY,
    val ltp: Double = 0.0,
    val isLive: Boolean = false,
    val timestamp: Long = 0L
)

data class Position(
    val symbol: String,
    val quantity: Int,
    val averagePrice: Double,
    val instrumentType: InstrumentType = InstrumentType.EQUITY,
    val lastPrice: Double = 0.0
) {
    val investedValue: Double
        get() = quantity * averagePrice

    val currentValue: Double
        get() = quantity * lastPrice

    val unrealizedPnl: Double
        get() = currentValue - investedValue
}

data class Trade(
    val id: Long,
    val symbol: String,
    val side: TradeSide,
    val quantity: Int,
    val price: Double,
    val charge: Double,
    val instrumentType: InstrumentType = InstrumentType.EQUITY,
    val timestamp: Long
)

enum class TradeSide {
    BUY,
    SELL
}
