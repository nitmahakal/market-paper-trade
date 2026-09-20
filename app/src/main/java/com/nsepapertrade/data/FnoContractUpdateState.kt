package com.nsepapertrade.data

data class FnoContractUpdateState(
    val isUpdating: Boolean = false,
    val lastResult: FnoContractUpdateResult? = null
)
