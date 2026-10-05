package com.nsepapertrade.data

import android.content.Context

class SpanRiskManager(
    context: Context
) {

    private val store =
        StoredSpanRiskStore(context)

    fun getAllRecords():
        List<StoredSpanRiskRecord> =
        store.loadRecords()

    fun getRecord(
        contractSymbol: String
    ): StoredSpanRiskRecord? {

        if (contractSymbol.isBlank()) {
            return null
        }

        return store
            .loadRecords()
            .firstOrNull {
                it.contractSymbol.equals(
                    contractSymbol.trim(),
                    ignoreCase = true
                )
            }
    }

    fun saveRecords(
        records: List<StoredSpanRiskRecord>
    ) {

        val validRecords =
            records.filter {
                isValid(it)
            }

        if (validRecords.isEmpty()) {
            throw IllegalArgumentException(
                "No valid SPAN risk records to save."
            )
        }

        store.saveRecords(
            validRecords
        )
    }

    fun updateRecord(
        record: StoredSpanRiskRecord
    ) {

        require(isValid(record)) {
            "Invalid SPAN risk record."
        }

        val existing =
            store.loadRecords()
                .filterNot {
                    it.contractSymbol.equals(
                        record.contractSymbol,
                        ignoreCase = true
                    )
                }

        store.saveRecords(
            existing + record
        )
    }

    fun clear() {
        store.clear()
    }

    private fun isValid(
        record: StoredSpanRiskRecord
    ): Boolean {

        if (record.contractSymbol.isBlank()) {
            return false
        }

        if (record.riskArray.size != 16) {
            return false
        }

        if (
            record.riskArray.any {
                it.isNaN() ||
                    it.isInfinite()
            }
        ) {
            return false
        }

        if (
            record.contractValueFactor <= 0.0 ||
            record.contractValueFactor.isNaN() ||
            record.contractValueFactor.isInfinite()
        ) {
            return false
        }

        return true
    }
}
