package com.nsepapertrade.data

class SpanMarginStoreUpdater(
    private val marginStore: FnoMarginStore
) {

    fun updateFromSpan(
        records: List<NseSpanMarginRecord>,
        contracts: List<com.nsepapertrade.model.FnoContract>
    ): Int {

        val matched =
            NseSpanContractMatcher()
                .match(
                    contracts = contracts,
                    records = records
                )

        val margins =
            NseSpanMarginMapper()
                .map(matched)

        if (margins.isEmpty()) {
            return 0
        }

        marginStore.saveMargins(
            margins
        )

        return margins.size
    }
}
