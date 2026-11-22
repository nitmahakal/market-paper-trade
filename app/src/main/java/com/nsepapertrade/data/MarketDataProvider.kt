package com.nsepapertrade.data

import com.nsepapertrade.model.Instrument

interface MarketDataProvider {

    suspend fun getQuote(
        instrument: Instrument
    ): MarketQuote?
}

data class MarketQuote(
    val symbol: String,
    val price: Double,
    val timestamp: Long,
    val source: MarketDataSource,
    // Today's opening price, 0.0 if the source does not give it.
    val openPrice: Double = 0.0
)
