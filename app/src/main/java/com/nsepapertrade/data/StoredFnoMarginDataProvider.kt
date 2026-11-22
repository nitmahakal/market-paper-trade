package com.nsepapertrade.data

import android.content.Context
import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoUnderlyingType

class StoredFnoMarginDataProvider(
    context: Context
) : FnoMarginDataProvider {

    companion object {
        // Used only when the SPAN file data is missing.
        private const val INDEX_APPROX_RATE = 0.12
        private const val STOCK_APPROX_RATE = 0.18
    }

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

        if (margin != null &&
            margin.futuresMargin > 0.0
        ) {
            return margin.futuresMargin * lots
        }

        // No SPAN data: approximate from the contract value.
        return approximateMargin(
            contract = contract,
            lots = lots,
            referencePrice = price
        )
    }

    override fun getOptionSellMargin(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): Double {

        val margin =
            getMargin(contract.symbol)

        if (margin != null &&
            margin.optionSellMargin > 0.0
        ) {
            return margin.optionSellMargin * lots
        }

        // No SPAN data: approximate from the strike value.
        val reference =
            if (contract.strikePrice > 0.0) {
                contract.strikePrice
            } else {
                price
            }

        return approximateMargin(
            contract = contract,
            lots = lots,
            referencePrice = reference
        )
    }

    private fun approximateMargin(
        contract: FnoContract,
        lots: Int,
        referencePrice: Double
    ): Double {

        require(referencePrice > 0.0 && contract.lotSize > 0) {
            "Cannot work out margin for ${contract.displayName}: " +
                "price or lot size is missing."
        }

        val rate =
            if (contract.underlyingType ==
                FnoUnderlyingType.INDEX
            ) {
                INDEX_APPROX_RATE
            } else {
                STOCK_APPROX_RATE
            }

        return referencePrice *
            contract.lotSize *
            lots *
            rate
    }

    private fun save() {
        store.saveMargins(
            margins.values.toList()
        )
    }
}
