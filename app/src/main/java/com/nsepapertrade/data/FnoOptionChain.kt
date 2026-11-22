package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract

data class FnoOptionChain(
    val underlying: String,
    val expiry: String,
    val rows: List<FnoOptionChainRow>,
    val timestamp: Long,
    // Price of the stock / index itself (0.0 if NSE did not send it).
    val underlyingValue: Double = 0.0
)

data class FnoOptionChainRow(
    val strikePrice: Double,
    val call: FnoOptionChainContract?,
    val put: FnoOptionChainContract?
)

data class FnoOptionChainContract(
    val contract: FnoContract,
    val ltp: Double = 0.0,
    val bidPrice: Double = 0.0,
    val askPrice: Double = 0.0,
    val openInterest: Long = 0L,
    val volume: Long = 0L
)
