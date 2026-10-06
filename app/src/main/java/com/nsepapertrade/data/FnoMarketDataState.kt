package com.nsepapertrade.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nsepapertrade.model.FnoContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FnoMarketDataState(
    private val provider: FnoMarketDataProvider,
    private val scope: CoroutineScope
) {

    var quote by mutableStateOf<MarketQuote?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf("")
        private set

    private var refreshJob: Job? = null

    fun start(
        contract: FnoContract,
        refreshIntervalMs: Long = 5_000L
    ) {
        stop()

        refreshJob =
            scope.launch(Dispatchers.IO) {

                while (isActive) {

                    loadQuote(contract)

                    delay(
                        refreshIntervalMs
                    )
                }
            }
    }

    fun stop() {
        refreshJob?.cancel()
        refreshJob = null
    }

    fun refresh(
        contract: FnoContract
    ) {
        scope.launch(Dispatchers.IO) {
            loadQuote(contract)
        }
    }

    private suspend fun loadQuote(
        contract: FnoContract
    ) {

        isLoading = true

        try {

            val result =
                provider.getQuote(contract)

            quote = result

            errorMessage =
                if (result == null) {
                    "No F&O market data available."
                } else {
                    ""
                }

        } catch (e: Exception) {

            errorMessage =
                e.message
                    ?: "F&O market data error."

        } finally {

            isLoading = false
        }
    }
}
