package com.nsepapertrade.data

import com.nsepapertrade.model.FnoPositionSide

class SpanRiskArrayCalculator {

    companion object {
        private const val SCENARIO_COUNT = 16

        private const val EXTREME_SCENARIO_START = 14

        private const val EXTREME_LOSS_COVERAGE = 0.35
    }

    fun calculateWorstLoss(
        riskArray: List<Double>,
        side: FnoPositionSide,
        quantity: Int,
        contractValueFactor: Double = 1.0
    ): Double {

        require(
            riskArray.size == SCENARIO_COUNT
        ) {
            "SPAN risk array must contain exactly 16 scenarios."
        }

        require(quantity > 0) {
            "Quantity must be greater than zero."
        }

        require(contractValueFactor > 0.0) {
            "Contract value factor must be greater than zero."
        }

        val scenarioLosses =
            riskArray.mapIndexed { index, value ->

                val signedLoss =
                    when (side) {

                        FnoPositionSide.LONG ->
                            value

                        FnoPositionSide.SHORT ->
                            -value
                    }

                if (
                    index >= EXTREME_SCENARIO_START
                ) {

                    signedLoss *
                        EXTREME_LOSS_COVERAGE

                } else {

                    signedLoss
                }
            }

        val worstLoss =
            scenarioLosses.maxOrNull()
                ?: 0.0

        return maxOf(
            0.0,
            worstLoss
        ) *
            quantity *
            contractValueFactor
    }

    fun calculateWorstLossPerUnit(
        riskArray: List<Double>,
        side: FnoPositionSide
    ): Double {

        require(
            riskArray.size == SCENARIO_COUNT
        ) {
            "SPAN risk array must contain exactly 16 scenarios."
        }

        val scenarioLosses =
            riskArray.mapIndexed { index, value ->

                val signedLoss =
                    when (side) {

                        FnoPositionSide.LONG ->
                            value

                        FnoPositionSide.SHORT ->
                            -value
                    }

                if (
                    index >= EXTREME_SCENARIO_START
                ) {

                    signedLoss *
                        EXTREME_LOSS_COVERAGE

                } else {

                    signedLoss
                }
            }

        return maxOf(
            0.0,
            scenarioLosses.maxOrNull()
                ?: 0.0
        )
    }
}
