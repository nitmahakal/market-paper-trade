package com.nsepapertrade.data

import com.nsepapertrade.model.Instrument

class MarketDataRepository(
    private val provider: MarketDataProvider
) {

    suspend fun getQuote(
        instrument: Instrument
    ): MarketQuote? {
        return provider.getQuote(instrument)
    }
}
