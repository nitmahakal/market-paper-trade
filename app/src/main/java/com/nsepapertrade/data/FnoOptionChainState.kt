
package com.nsepapertrade.data

data class FnoOptionChainState(
    val isLoading: Boolean = false,
    val chain: FnoOptionChain? = null,
    val errorMessage: String = ""
)

