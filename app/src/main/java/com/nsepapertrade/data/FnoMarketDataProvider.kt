package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract

interface FnoMarketDataProvider {

    suspend fun getQuote(
        contract: FnoContract
    ): MarketQuote?
}
