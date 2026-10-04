package com.nsepapertrade.data

import android.content.Context
import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.FnoMargin
import com.nsepapertrade.model.FnoPortfolioSnapshot
import com.nsepapertrade.model.FnoPosition
import com.nsepapertrade.model.FnoPositionSide
import com.nsepapertrade.model.FnoTrade
import com.nsepapertrade.model.OptionType
import com.nsepapertrade.model.TradeSide

class FnoTradeEngine(
    context: Context,
    private val marginCalculator: FnoMarginCalculator =
        DefaultFnoMarginCalculator(
            UnavailableFnoMarginDataProvider()
        )
) {

    companion object {
        private const val INITIAL_CAPITAL = 1_000_000.0

        private const val BROKERAGE_RATE = 0.0004
        private const val EXTRA_EXPENSE_RATE = 0.25

        private const val CASH_KEY =
            "fno_available_cash"

        private const val MARGIN_KEY =
            "fno_reserved_margin"

        private const val REALIZED_PNL_KEY =
            "fno_realized_pnl"
    }

    private val storage =
        PaperTradeStorage(context)

    private val repository =
        FnoTradeRepository(
            FnoTradePersistence(storage)
        )

    private var availableCash =
        storage.getDouble(
            CASH_KEY,
            INITIAL_CAPITAL
        )

    private var reservedMargin =
        storage.getDouble(
            MARGIN_KEY,
            0.0
        )

    private var realizedPnl =
        storage.getDouble(
            REALIZED_PNL_KEY,
            0.0
        )

    fun getAvailableCash(): Double =
        availableCash

    fun getReservedMargin(): Double =
        reservedMargin

    fun getRealizedPnl(): Double =
        realizedPnl

    fun getPositions(): List<FnoPosition> =
        repository.getPositions()

    fun getTrades(): List<FnoTrade> =
        repository.getTrades()

    fun getUnrealizedPnl(): Double =
        repository
            .getPositions()
            .sumOf {
                it.unrealizedPnl
            }

    fun getPortfolioSnapshot():
        FnoPortfolioSnapshot {

        return FnoPortfolioSnapshot(
            availableCash = availableCash,
            reservedMargin = reservedMargin,
            realizedPnl = realizedPnl,
            unrealizedPnl = getUnrealizedPnl()
        )
    }

    fun buy(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): Result<Unit> {

        return executeTrade(
            contract = contract,
            side = TradeSide.BUY,
            lots = lots,
            price = price
        )
    }

    fun sell(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): Result<Unit> {

        return executeTrade(
            contract = contract,
            side = TradeSide.SELL,
            lots = lots,
            price = price
        )
    }

    private fun executeTrade(
        contract: FnoContract,
        side: TradeSide,
        lots: Int,
        price: Double
    ): Result<Unit> {

        return try {

            require(contract.isActive) {
                "This F&O contract is inactive."
            }

            require(contract.lotSize > 0) {
                "Invalid contract lot size."
            }

            require(lots > 0) {
                "Lots must be greater than zero."
            }

            require(price > 0.0) {
                "Price must be greater than zero."
            }

            val positionSide =
                when (side) {
                    TradeSide.BUY ->
                        FnoPositionSide.LONG

                    TradeSide.SELL ->
                        FnoPositionSide.SHORT
                }

            val oppositeSide =
                when (positionSide) {
                    FnoPositionSide.LONG ->
                        FnoPositionSide.SHORT

                    FnoPositionSide.SHORT ->
                        FnoPositionSide.LONG
                }

            val oppositePosition =
                repository.getPositions()
                    .firstOrNull {
                        it.contract.symbol.equals(
                            contract.symbol,
                            ignoreCase = true
                        ) &&
                            it.side == oppositeSide
                    }

            if (oppositePosition != null) {

                val lotsToClose =
                    minOf(
                        lots,
                        oppositePosition.lots
                    )

                closePosition(
                    position = oppositePosition,
                    lots = lotsToClose,
                    price = price
                )

                val remainingLots =
                    lots - lotsToClose

                if (remainingLots > 0) {
                    openPosition(
                        contract = contract,
                        side = positionSide,
                        lots = remainingLots,
                        price = price
                    )
                }

            } else {

                openPosition(
                    contract = contract,
                    side = positionSide,
                    lots = lots,
                    price = price
                )
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun openPosition(
        contract: FnoContract,
        side: FnoPositionSide,
        lots: Int,
        price: Double
    ) {

        val margin =
            calculateOpeningMargin(
                contract = contract,
                side = side,
                lots = lots,
                price = price
            )

        val charge =
            calculateCharge(
                contract = contract,
                lots = lots,
                price = price
            )

        val totalRequired =
            margin.requiredAmount + charge

        require(availableCash >= totalRequired) {
            "Insufficient available funds."
        }

        val existing =
            repository.getPositions()
                .firstOrNull {
                    it.contract.symbol.equals(
                        contract.symbol,
                        ignoreCase = true
                    ) &&
                        it.side == side
                }

        val newPosition =
            if (existing == null) {

                FnoPosition(
                    contract = contract,
                    side = side,
                    lots = lots,
                    averagePrice = price,
                    lastPrice = price,
                    reservedMargin =
                        margin.requiredAmount
                )

            } else {

                val totalLots =
                    existing.lots + lots

                val averagePrice =
                    (
                        existing.averagePrice *
                            existing.lots +
                            price * lots
                        ) / totalLots

                existing.copy(
                    lots = totalLots,
                    averagePrice = averagePrice,
                    lastPrice = price,
                    reservedMargin =
                        existing.reservedMargin +
                            margin.requiredAmount
                )
            }

        availableCash -= totalRequired
        reservedMargin += margin.requiredAmount

        repository.addPosition(newPosition)

        addTrade(
            contract = contract,
            side =
                when (side) {
                    FnoPositionSide.LONG ->
                        TradeSide.BUY

                    FnoPositionSide.SHORT ->
                        TradeSide.SELL
                },
            positionSide = side,
            lots = lots,
            price = price,
            charge = charge
        )

        saveFinancialState()
    }

    private fun closePosition(
        position: FnoPosition,
        lots: Int,
        price: Double
    ) {

        val quantity =
            lots * position.contract.lotSize

        val pnlPerUnit =
            when (position.side) {

                FnoPositionSide.LONG ->
                    price - position.averagePrice

                FnoPositionSide.SHORT ->
                    position.averagePrice - price
            }

        val grossPnl =
            pnlPerUnit * quantity

        val charge =
            calculateCharge(
                contract = position.contract,
                lots = lots,
                price = price
            )

        val releasedMargin =
            if (position.lots > 0) {
                position.reservedMargin *
                    lots.toDouble() /
                    position.lots.toDouble()
            } else {
                0.0
            }

        val netPnl =
            grossPnl - charge

        availableCash +=
            releasedMargin + netPnl

        reservedMargin -= releasedMargin

        if (reservedMargin < 0.0) {
            reservedMargin = 0.0
        }

        val remainingLots =
            position.lots - lots

        if (remainingLots <= 0) {

            repository.removePosition(
                contractSymbol =
                    position.contract.symbol,
                side = position.side
            )

        } else {

            repository.addPosition(
                position.copy(
                    lots = remainingLots,
                    lastPrice = price,
                    reservedMargin =
                        position.reservedMargin -
                            releasedMargin
                )
            )
        }

        realizedPnl += netPnl

        addTrade(
            contract = position.contract,
            side =
                when (position.side) {
                    FnoPositionSide.LONG ->
                        TradeSide.SELL

                    FnoPositionSide.SHORT ->
                        TradeSide.BUY
                },
            positionSide = position.side,
            lots = lots,
            price = price,
            charge = charge
        )

        saveFinancialState()
    }

    private fun calculateOpeningMargin(
        contract: FnoContract,
        side: FnoPositionSide,
        lots: Int,
        price: Double
    ): FnoMargin {

        return when (contract.contractType) {

            FnoContractType.FUTURE -> {
                marginCalculator.calculateFutureMargin(
                    contract = contract,
                    lots = lots,
                    price = price
                )
            }

            FnoContractType.OPTION -> {

                when (side) {

                    FnoPositionSide.LONG -> {
                        marginCalculator
                            .calculateOptionBuyRequirement(
                                contract = contract,
                                lots = lots,
                                premium = price
                            )
                    }

                    FnoPositionSide.SHORT -> {
                        marginCalculator
                            .calculateOptionSellMargin(
                                contract = contract,
                                lots = lots,
                                price = price
                            )
                    }
                }
            }
        }
    }

    private fun calculateCharge(
        contract: FnoContract,
        lots: Int,
        price: Double
    ): Double {

        val quantity =
            lots * contract.lotSize

        val turnover =
            quantity * price

        return turnover *
            BROKERAGE_RATE *
            (1.0 + EXTRA_EXPENSE_RATE)
    }

    private fun addTrade(
        contract: FnoContract,
        side: TradeSide,
        positionSide: FnoPositionSide,
        lots: Int,
        price: Double,
        charge: Double
    ) {

        repository.addTrade(
            FnoTrade(
                id = System.currentTimeMillis(),
                contract = contract,
                side = side,
                positionSide = positionSide,
                lots = lots,
                price = price,
                charge = charge,
                timestamp =
                    System.currentTimeMillis()
            )
        )
    }

    fun updateMarketPrice(
        contractSymbol: String,
        price: Double
    ) {

        if (
            contractSymbol.isBlank() ||
            price <= 0.0
        ) {
            return
        }

        repository
            .getPositions()
            .filter {
                it.contract.symbol.equals(
                    contractSymbol,
                    ignoreCase = true
                )
            }
            .forEach { position ->

                repository.addPosition(
                    position.copy(
                        lastPrice = price
                    )
                )
            }
    }

    private fun saveFinancialState() {

        storage.putDouble(
            CASH_KEY,
            availableCash
        )

        storage.putDouble(
            MARGIN_KEY,
            reservedMargin
        )

        storage.putDouble(
            REALIZED_PNL_KEY,
            realizedPnl
        )
    }
}
