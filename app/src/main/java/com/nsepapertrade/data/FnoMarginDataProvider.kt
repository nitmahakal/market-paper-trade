package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract

interface FnoMarginDataProvider {

    fun getFutureMargin(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): Double

    fun getOptionSellMargin(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): Double
}
