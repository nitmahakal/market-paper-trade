package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract

class StoredFnoMarginDataProvider :
    FnoMarginDataProvider {

    private val margins =
        mutableMapOf<String, FnoContractMargin>()

    fun update(
        margin: FnoContractMargin
    ) {
        margins[
            margin.contractSymbol.uppercase()
        ] = margin
    }

    fun updateAll(
        newMargins: List<FnoContractMargin>
    ) {
        newMargins.forEach { margin ->
            update(margin)
        }
    }

    fun getMargin(
        contractSymbol: String
    ): FnoContractMargin? =
        margins[
            contractSymbol.uppercase()
        ]

    fun getAllMargins():
        List<FnoContractMargin> =
        margins.values.toList()

    override fun getFutureMargin(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): Double {

        val margin =
            getMargin(contract.symbol)

        require(
            margin != null &&
                margin.futuresMargin > 0.0
        ) {
            "F&O futures margin data is not available for ${contract.displayName}."
        }

        return margin.futuresMargin * lots
    }

    override fun getOptionSellMargin(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): Double {

        val margin =
            getMargin(contract.symbol)

        require(
            margin != null &&
                margin.optionSellMargin > 0.0
        ) {
            "F&O option-writing margin data is not available for ${contract.displayName}."
        }

        return margin.optionSellMargin * lots
    }
}
