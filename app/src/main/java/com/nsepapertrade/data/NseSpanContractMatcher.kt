package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract

class NseSpanContractMatcher {

    fun match(
        contracts: List<FnoContract>,
        records: List<NseSpanMarginRecord>
    ): List<NseSpanMarginRecord> {

        if (
            contracts.isEmpty() ||
            records.isEmpty()
        ) {
            return emptyList()
        }

        return records.mapNotNull { record ->

            val contract =
                contracts.firstOrNull {
                    it.symbol.equals(
                        record.contractSymbol,
                        ignoreCase = true
                    )
                }

            contract?.let {
                record.copy(
                    lotSize = it.lotSize
                )
            }
        }
    }

    fun findMatch(
        contract: FnoContract,
        records: List<NseSpanMarginRecord>
    ): NseSpanMarginRecord? {

        return records.firstOrNull {
            it.contractSymbol.equals(
                contract.symbol,
                ignoreCase = true
            )
        }
    }
}
