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
        val diagnostics = mutableListOf<String>()
    
        for (date in datesToTry) {
            try {
                val lines = downloader.download(date)
    
                val headerIndex = lines.indexOfFirst {
                    it.contains("FinInstrmNm", ignoreCase = true) &&
                        it.contains("TckrSymb", ignoreCase = true)
                }
    
                val contracts = parser.parse(lines)
    
                diagnostics +=
                    "$date lines=${lines.size} header=$headerIndex parsed=${contracts.size}"
    
                if (contracts.isNotEmpty()) {
                    return contracts
                }
            } catch (e: Exception) {
                diagnostics +=
                    "$date error=${e.message ?: "unknown"}"
                lastError = e
            }
        }
    
        throw IllegalStateException(
            "F&O diagnostic: " +
                diagnostics.joinToString(" | ") +
                (
                    lastError?.message?.let {
                        " | lastError=$it"
                    } ?: ""
                )
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
