package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract

class StoredFnoMarginDataProvider : FnoMarginDataProvider {

    private var snapshot =
        FnoMarginSnapshot(
            futureMargin = 0.0,
            optionSellMargin = 0.0,
            timestamp = 0L
        )

    fun update(
        newSnapshot: FnoMarginSnapshot
    ) {
        snapshot = newSnapshot
    }

    fun getSnapshot(): FnoMarginSnapshot =
        snapshot

    override fun getFutureMargin(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): Double {

        require(snapshot.futureMargin > 0.0) {
            "F&O futures margin data is not available."
        }

        return snapshot.futureMargin * lots
    }

    override fun getOptionSellMargin(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): Double {

        require(snapshot.optionSellMargin > 0.0) {
            "F&O option-writing margin data is not available."
        }

        return snapshot.optionSellMargin * lots
    }
}
