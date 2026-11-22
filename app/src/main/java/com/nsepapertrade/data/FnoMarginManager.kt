package com.nsepapertrade.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FnoMarginManager(
    context: Context,
    private val provider: FnoMarginProvider
) {

    private val store =
        FnoMarginStore(context)

    fun getMargins():
        List<FnoContractMargin> =
        store.loadMargins()

    // Network and file work must never run on the main thread.
    suspend fun update():
        FnoMarginUpdateResult {

        return withContext(Dispatchers.IO) {
            doUpdate()
        }
    }

    private suspend fun doUpdate():
        FnoMarginUpdateResult {

        return try {

            val downloadedMargins =
                provider.fetchMargins()

            if (downloadedMargins.isEmpty()) {

                return FnoMarginUpdateResult(
                    success = false,
                    marginCount = 0,
                    updateTime = 0L,
                    message =
                        "No valid F&O margin data received."
                )
            }

            store.saveMargins(
                downloadedMargins
            )

            val updateTime =
                downloadedMargins
                    .maxOfOrNull {
                        it.timestamp
                    }
                    ?: System.currentTimeMillis()

            FnoMarginUpdateResult(
                success = true,
                marginCount =
                    downloadedMargins.size,
                updateTime = updateTime,
                message =
                    "F&O margin data updated: " +
                        downloadedMargins.size
            )

        } catch (e: Exception) {

            FnoMarginUpdateResult(
                success = false,
                marginCount = 0,
                updateTime = 0L,
                message =
                    e.message
                        ?: e.javaClass.simpleName
                        ?: "F&O margin update failed."
            )
        }
    }
}
