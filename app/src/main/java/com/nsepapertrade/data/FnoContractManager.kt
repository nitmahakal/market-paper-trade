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
            val contracts = provider.fetchContracts()

            if (contracts.isEmpty()) {
                return Result.failure(
                    IllegalStateException("No F&O contracts received.")
                )
            }

            store.saveContracts(contracts)

            Result.success(contracts.size)
        } catch (e: Exception) {
            Result.failure(
                e.message?.let { IllegalStateException(it) }
                    ?: IllegalStateException("F&O contract update failed.")
            )
        }
    }
}
