package com.nsepapertrade.data

class NseSpanRiskRecordMapper {

    fun map(
        records: List<NseSpanMarginRecord>
    ): List<StoredSpanRiskRecord> {

        return records
            .mapNotNull { record ->
                mapSingle(record)
            }
    }

    private fun mapSingle(
        record: NseSpanMarginRecord
    ): StoredSpanRiskRecord? {

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

        return StoredSpanRiskRecord(
            contractSymbol =
                record.contractSymbol.trim(),

            riskArray =
                record.riskArray.toList(),

            contractValueFactor =
                calculateContractValueFactor(
                    record
                ),

            timestamp =
                record.timestamp
        )
    }

    private fun calculateContractValueFactor(
        record: NseSpanMarginRecord
    ): Double {

        if (record.lotSize > 0) {
            return record.lotSize.toDouble()
        }

        return 1.0
    }
}
