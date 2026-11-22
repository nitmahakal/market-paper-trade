package com.nsepapertrade.data

import android.content.Context
import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.OptionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FnoContractManager(
    context: Context,
    private val provider: FnoContractProvider
) {

    private val store = FnoContractStore(context)

    fun getContracts(): List<FnoContract> =
        store.loadContracts()

    fun getLastUpdateTime(): Long =
        store.getLastUpdateTime()

    // Network and file work must never run on the main thread.
    suspend fun update(): FnoContractUpdateResult {
        return withContext(Dispatchers.IO) {
            doUpdate()
        }
    }

    private suspend fun doUpdate(): FnoContractUpdateResult {
        return try {
            val downloadedContracts =
                provider.fetchContracts()

            val validContracts =
                downloadedContracts
                    .filter { isValidContract(it) }
                    .map { it.copy(isActive = true) }

            if (validContracts.isEmpty()) {
                return FnoContractUpdateResult(
                    success = false,
                    contractCount = 0,
                    updateTime = 0L,
                    message =
                        "NSE returned no valid F&O contracts."
                )
            }

            val existingContracts =
                store.loadContracts()
                    .map { contract ->
                        if (isExpired(contract.expiry)) {
                            contract.copy(isActive = false)
                        } else {
                            contract
                        }
                    }

            store.saveContracts(
                existingContracts + validContracts
            )

            FnoContractUpdateResult(
                success = true,
                contractCount = validContracts.size,
                updateTime = store.getLastUpdateTime(),
                message =
                    "F&O contracts updated: ${validContracts.size}"
            )

        } catch (e: Exception) {
            FnoContractUpdateResult(
                success = false,
                contractCount = 0,
                updateTime = 0L,
                message =
                    e.message
                        ?: e.javaClass.simpleName
                        ?: "F&O contract update failed."
            )
        }
    }

    private fun isValidContract(
        contract: FnoContract
    ): Boolean {

        if (contract.underlying.isBlank()) {
            return false
        }

        if (contract.expiry.isBlank()) {
            return false
        }

        if (contract.lotSize <= 0) {
            return false
        }

        return when (contract.contractType) {

            FnoContractType.FUTURE ->
                true

            FnoContractType.OPTION ->
                contract.strikePrice >= 0.0 &&
                    contract.optionType != OptionType.NONE
        }
    }

    private fun isExpired(
        expiry: String
    ): Boolean {

        val formats = listOf(
            "dd-MMM-yyyy",
            "ddMMMyyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd"
        )

        for (pattern in formats) {
            try {
                val formatter =
                    SimpleDateFormat(
                        pattern,
                        Locale.US
                    )

                formatter.isLenient = false

                val expiryDate =
                    formatter.parse(expiry)
                        ?: continue

                val today =
                    SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                    ).format(Date())

                val todayDate =
                    SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                    ).parse(today)
                        ?: continue

                return expiryDate.before(todayDate)

            } catch (_: Exception) {
                // Try the next supported format.
            }
        }

        return false
    }
}
