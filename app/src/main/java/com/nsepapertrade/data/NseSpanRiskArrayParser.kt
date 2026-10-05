package com.nsepapertrade.data

class NseSpanRiskArrayParser {

    companion object {
        private const val EXPECTED_SCENARIO_COUNT = 16
    }

    fun parseRiskArray(
        values: List<String>
    ): List<Double> {

        val riskValues =
            values.mapNotNull { value ->
                value.trim().toDoubleOrNull()
            }

        require(
            riskValues.size == EXPECTED_SCENARIO_COUNT
        ) {
            "Invalid SPAN risk array: expected " +
                "$EXPECTED_SCENARIO_COUNT scenarios, " +
                "found ${riskValues.size}."
        }

        return riskValues
    }

    fun isValidRiskArray(
        values: List<Double>
    ): Boolean {
        return values.size ==
            EXPECTED_SCENARIO_COUNT &&
            values.all { it.isFinite() }
    }
}
