package com.nsepapertrade.data

class NseSpanMarginMapper {

    fun map(
        records: List<NseSpanMarginRecord>
    ): List<FnoContractMargin> {

        return records
            .mapNotNull { record ->
                mapSingle(record)
            }
    }

    private fun mapSingle(
        record: NseSpanMarginRecord
    ): FnoContractMargin? {

        if (
            record.contractSymbol.isBlank()
        ) {
            return null
        }

        if (
            record.riskArray.size != 16
        ) {
            return null
        }

        if (
            record.riskArray.any {
                it.isNaN() ||
                    it.isInfinite()
            }
        ) {
            return null
        }

        val worstLoss =
            record.riskArray
                .maxOrNull()
                ?: return null

        val margin =
            maxOf(
                0.0,
                worstLoss
            ) *
                record.lotSize

        return FnoContractMargin(
            contractSymbol =
                record.contractSymbol,

            futuresMargin =
                if (
                    record.contractType
                        .equals(
                            "FUTURE",
                            ignoreCase = true
                        )
                ) {
                    margin
                } else {
                    0.0
                },

            optionSellMargin =
                if (
                    record.contractType
                        .equals(
                            "OPTION",
                            ignoreCase = true
                        )
                ) {
                    margin
                } else {
                    0.0
                },

            timestamp =
                record.timestamp
        )
    }
}
