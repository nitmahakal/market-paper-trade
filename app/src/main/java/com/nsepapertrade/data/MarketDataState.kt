package com.nsepapertrade.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nsepapertrade.model.Instrument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MarketDataState(
    private val provider: MarketDataProvider,
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
        instrument: Instrument,
        refreshIntervalMs: Long = 5_000L
    ) {
        stop()

        refreshJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                loadQuote(instrument)
                delay(refreshIntervalMs)
            }
        }
    }

    fun stop() {
        refreshJob?.cancel()
        refreshJob = null
    }

    fun refresh(instrument: Instrument) {
        scope.launch(Dispatchers.IO) {
            loadQuote(instrument)
        }
    }

    private suspend fun loadQuote(
        instrument: Instrument
    ) {
        isLoading = true

        try {
            val result = provider.getQuote(instrument)

            quote = result

            errorMessage = if (result == null) {
                "No market data available."
            } else {
                ""
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Market data error."
        } finally {
            isLoading = false
        }
    }
}
