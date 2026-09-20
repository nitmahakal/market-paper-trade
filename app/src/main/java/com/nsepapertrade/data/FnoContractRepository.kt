package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.FnoUnderlyingType

class FnoContractRepository(
    private val contracts: List<FnoContract> = emptyList()
) {

    fun getAll(): List<FnoContract> =
        contracts.sortedWith(
            compareBy<FnoContract> { it.underlying.uppercase() }
                .thenBy { it.expiry }
                .thenBy { it.contractType.name }
                .thenBy { it.strikePrice }
                .thenBy { it.optionType.name }
        )

    fun search(
        query: String,
        contractType: FnoContractType? = null,
        underlyingType: FnoUnderlyingType? = null
    ): List<FnoContract> {
        val normalizedQuery = query.trim().uppercase()

        return getAll().filter { contract ->
            val matchesQuery =
                normalizedQuery.isBlank() ||
                    contract.underlying.uppercase().contains(normalizedQuery)

            val matchesContractType =
                contractType == null ||
                    contract.contractType == contractType

            val matchesUnderlyingType =
                underlyingType == null ||
                    contract.underlyingType == underlyingType

            matchesQuery &&
                matchesContractType &&
                matchesUnderlyingType
        }
    }

    fun getFutures(query: String = ""): List<FnoContract> =
        search(
            query = query,
            contractType = FnoContractType.FUTURE
        )

    fun getOptions(query: String = ""): List<FnoContract> =
        search(
            query = query,
            contractType = FnoContractType.OPTION
        )

    fun findExact(contract: FnoContract): FnoContract? =
        contracts.firstOrNull {
            it.underlying.equals(contract.underlying, ignoreCase = true) &&
                it.underlyingType == contract.underlyingType &&
                it.contractType == contract.contractType &&
                it.expiry == contract.expiry &&
                it.strikePrice == contract.strikePrice &&
                it.optionType == contract.optionType &&
                it.lotSize == contract.lotSize
        }
}
