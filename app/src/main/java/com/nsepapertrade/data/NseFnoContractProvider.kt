package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NseFnoContractProvider(
    private val downloader: NseFnoContractDownloader = NseFnoContractDownloader(),
    private val parser: NseFnoContractParser = NseFnoContractParser()
) : FnoContractProvider {

    override suspend fun fetchContracts(): List<FnoContract> {
        val datesToTry = buildDateCandidates()

        var lastError: Exception? = null

        for (date in datesToTry) {
            try {
                val lines = downloader.download(date)

                val contracts = parser.parse(lines)

                if (contracts.isNotEmpty()) {
                    return contracts
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        throw IllegalStateException(
            lastError?.message
                ?: "No valid NSE F&O contract data found."
        )
    }

    private fun buildDateCandidates(): List<String> {
        val formatter =
            SimpleDateFormat("ddMMyyyy", Locale.US)

        val calendar = Calendar.getInstance()

        return buildList {
            repeat(7) {
                add(formatter.format(calendar.time))
                calendar.add(Calendar.DAY_OF_MONTH, -1)
            }
        }.distinct()
    }
}
