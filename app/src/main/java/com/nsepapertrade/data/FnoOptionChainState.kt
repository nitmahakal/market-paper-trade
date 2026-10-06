package com.nsepapertrade.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FnoOptionChainState(
    private val provider: FnoOptionChainProvider,
    private val scope: CoroutineScope
) {

    var isLoading: Boolean = false
        private set

    var chain: FnoOptionChain? = null
        private set

    var errorMessage: String = ""
        private set

    fun load(
        underlying: String,
        expiry: String
    ) {
        scope.launch(Dispatchers.IO) {

            isLoading = true
            errorMessage = ""

            try {

                val result =
                    provider.getOptionChain(
                        underlying = underlying,
                        expiry = expiry
                    )

                chain = result

                if (result == null) {
                    errorMessage =
                        "No option chain data available."
                }

            } catch (e: Exception) {

                errorMessage =
                    e.message
                        ?: "Option chain error."

            } finally {

                isLoading = false
            }
        }
    }

    fun clear() {
        chain = null
        errorMessage = ""
        isLoading = false
    }
}
