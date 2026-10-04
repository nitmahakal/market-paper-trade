package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.FnoMargin
import com.nsepapertrade.model.FnoMarginType

class DefaultFnoMarginCalculator(
    private val marginDataProvider: FnoMarginDataProvider
) : FnoMarginCalculator {

    override fun calculateFutureMargin(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): FnoMargin {

        require(contract.contractType == FnoContractType.FUTURE) {
            "Future margin requires a futures contract."
        }

        require(lots > 0) {
            "Lots must be greater than zero."
        }

        require(price >= 0.0) {
            "Price cannot be negative."
        }

        val amount =
            marginDataProvider.getFutureMargin(
                contract = contract,
                lots = lots,
                price = price
            )

        return FnoMargin(
            marginType = FnoMarginType.FUTURE,
            requiredAmount = amount
        )
    }

    override fun calculateOptionBuyRequirement(
        contract: FnoContract,
        lots: Int,
        premium: Double
    ): FnoMargin {

        require(contract.contractType == FnoContractType.OPTION) {
            "Option buy requirement requires an option contract."
        }

        require(lots > 0) {
            "Lots must be greater than zero."
        }

        require(premium >= 0.0) {
            "Premium cannot be negative."
        }

        val quantity =
            lots * contract.lotSize

        val amount =
            quantity * premium

        return FnoMargin(
            marginType = FnoMarginType.OPTION_BUY,
            requiredAmount = amount
        )
    }

    override fun calculateOptionSellMargin(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): FnoMargin {

        require(contract.contractType == FnoContractType.OPTION) {
            "Option sell margin requires an option contract."
        }

        require(lots > 0) {
            "Lots must be greater than zero."
        }

        require(price >= 0.0) {
            "Price cannot be negative."
        }

        val amount =
            marginDataProvider.getOptionSellMargin(
                contract = contract,
                lots = lots,
                price = price
            )

        return FnoMargin(
            marginType = FnoMarginType.OPTION_SELL,
            requiredAmount = amount
        )
    }
}
