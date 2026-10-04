package com.nsepapertrade.data

import android.content.Context
import com.nsepapertrade.model.FnoPortfolioSnapshot
import com.nsepapertrade.model.FnoPosition
import com.nsepapertrade.model.FnoTrade

class FnoTradeEngine(
    context: Context,
    marginCalculator: FnoMarginCalculator =
        DefaultFnoMarginCalculator(
            UnavailableFnoMarginDataProvider()
        )
) {

    private val storage =
        PaperTradeStorage(context)

    private val repository =
        FnoTradeRepository(
            FnoTradePersistence(storage)
        )

    @Suppress("UNUSED_PARAMETER")
    private val configuredMarginCalculator =
        marginCalculator

    private var availableCash =
        storage.getDouble(
            CASH_KEY,
            1_000_000.0
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

        val positions =
            repository.getPositions()

        positions
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

    companion object {
        private const val CASH_KEY =
            "fno_available_cash"

        private const val MARGIN_KEY =
            "fno_reserved_margin"

        private const val REALIZED_PNL_KEY =
            "fno_realized_pnl"
    }
}
