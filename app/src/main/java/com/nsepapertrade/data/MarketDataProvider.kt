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
    val isDelayed: Boolean,
    val source: String
)
