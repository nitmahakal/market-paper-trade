package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract

class UnavailableFnoMarginDataProvider :
    FnoMarginDataProvider {

    override fun getFutureMargin(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): Double {
        throw IllegalStateException(
            "F&O futures margin data is not available."
        )
    }

    override fun getOptionSellMargin(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): Double {
        throw IllegalStateException(
            "F&O option-writing margin data is not available."
        )
    }
}
