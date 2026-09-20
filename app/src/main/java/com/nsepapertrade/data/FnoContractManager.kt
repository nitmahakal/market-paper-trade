package com.nsepapertrade.data

import android.content.Context
import com.nsepapertrade.model.FnoContract

class FnoContractManager(
    context: Context,
    private val provider: FnoContractProvider
) {

    private val store = FnoContractStore(context)

    fun getContracts(): List<FnoContract> =
        store.loadContracts()

    fun getLastUpdateTime(): Long =
        store.getLastUpdateTime()

    suspend fun update(): Result<Int> {
        return try {
            val downloadedContracts = provider.fetchContracts()

            val validContracts =
                downloadedContracts.filter { isValidContract(it) }

            if (validContracts.isEmpty()) {
                return Result.failure(
                    IllegalStateException(
                        "NSE returned no valid F&O contracts."
                    )
                )
            }

            store.saveContracts(validContracts)

            Result.success(validContracts.size)
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "F&O contract update failed."
                )
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
            com.nsepapertrade.model.FnoContractType.FUTURE -> true

            com.nsepapertrade.model.FnoContractType.OPTION ->
                contract.strikePrice >= 0.0 &&
                    contract.optionType !=
                    com.nsepapertrade.model.OptionType.NONE
        }
    }
}
