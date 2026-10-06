
package com.nsepapertrade.data

interface FnoOptionChainProvider {
    suspend fun getOptionChain(
        underlying: String,
        expiry: String
    ): FnoOptionChain?
}

