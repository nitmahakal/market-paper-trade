package com.nsepapertrade.data

data class FnoContractUpdateResult(
    val success: Boolean,
    val contractCount: Int,
    val updateTime: Long,
    val message: String
)
