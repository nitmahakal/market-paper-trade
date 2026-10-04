package com.nsepapertrade.data

interface FnoMarginProvider {

    suspend fun fetchMargins(): List<FnoContractMargin>
}
