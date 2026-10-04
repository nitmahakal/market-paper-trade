package com.nsepapertrade.data

import android.content.Context
import com.nsepapertrade.model.FnoContract

class StoredFnoMarginDataProvider(
    context: Context
) : FnoMarginDataProvider {

    private val store =
        FnoMarginStore(context)

    private val margins =
        mutableMapOf<String, FnoContractMargin>()

    init {
        store.loadMargins()
            .forEach { margin ->
                margins[
                    margin.contractSymbol.uppercase()
                ] = margin
            }
    }

    fun update(
        margin: FnoContractMargin
    ) {
        margins[
            margin.contractSymbol.uppercase()
        ] = margin

        save()
    }

    fun updateAll(
        newMargins: List<FnoContractMargin>
    ) {
        newMargins.forEach { margin ->
            margins[
                margin.contractSymbol.uppercase()
            ] = margin
        }

        save()
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

    private fun save() {
        store.saveMargins(
            margins.values.toList()
        )
    }
}
