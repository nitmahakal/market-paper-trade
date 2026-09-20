package com.nsepapertrade.data

import android.content.Context
import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.OptionType

class FnoContractManager(
    context: Context,
    private val provider: FnoContractProvider
) {

    private val store = FnoContractStore(context)

    fun getContracts(): List<FnoContract> =
        store.loadContracts()

    fun getLastUpdateTime(): Long =
        store.getLastUpdateTime()

    suspend fun update(): FnoContractUpdateResult {
        return try {
            val downloadedContracts = provider.fetchContracts()

            val validContracts =
                downloadedContracts.filter { isValidContract(it) }

            if (validContracts.isEmpty()) {
                return FnoContractUpdateResult(
                    success = false,
                    contractCount = 0,
                    updateTime = 0L,
                    message = "NSE returned no valid F&O contracts."
                )
            }

            store.saveContracts(validContracts)

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
                    e.message ?: "F&O contract update failed."
            )
        }
    }

    private fun isValidContract(
        contract: FnoContract
    ): Boolean {

        if (contract.underlying.isBlank()) return false
        if (contract.expiry.isBlank()) return false
        if (contract.lotSize <= 0) return false

        return when (contract.contractType) {
            FnoContractType.FUTURE -> true

            FnoContractType.OPTION ->
                contract.strikePrice >= 0.0 &&
                    contract.optionType != OptionType.NONE
        }
    }
}
