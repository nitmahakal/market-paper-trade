package com.nsepapertrade.data

import com.nsepapertrade.model.Instrument

class ManualMarketDataProvider : MarketDataProvider {

    private val prices = mutableMapOf<String, Double>()

    fun setPrice(
        instrument: Instrument,
        price: Double
    ) {
        if (price > 0.0) {
            prices[instrument.symbol] = price
        }
    }

    override suspend fun getQuote(
        instrument: Instrument
    ): MarketQuote? {

        val price = prices[instrument.symbol]
            ?: return null

        return MarketQuote(
            symbol = instrument.symbol,
            price = price,
            timestamp = System.currentTimeMillis(),
            isDelayed = true,
            source = "Manual"
        )
    }
}
