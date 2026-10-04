package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoMargin

interface FnoMarginCalculator {

    fun calculateFutureMargin(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): FnoMargin

    fun calculateOptionBuyRequirement(
        contract: FnoContract,
        lots: Int,
        premium: Double
    ): FnoMargin

    fun calculateOptionSellMargin(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): FnoMargin
}
