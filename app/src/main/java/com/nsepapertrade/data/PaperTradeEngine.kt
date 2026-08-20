package com.nsepapertrade.data

import com.nsepapertrade.model.Position
import com.nsepapertrade.model.Trade
import com.nsepapertrade.model.TradeSide

class PaperTradeEngine(
    private val repository: PaperTradeRepository = PaperTradeRepository()
) {

    companion object {
        const val INITIAL_CAPITAL = 1_000_000.0

        // Simple paper-trading charge model:
        // Brokerage = 0.04%
        // Other expenses = 25% of brokerage
        const val BROKERAGE_RATE = 0.0004
        const val EXTRA_EXPENSE_RATE = 0.25
    }

    private var availableCash = INITIAL_CAPITAL

    fun getAvailableCash(): Double {
        return availableCash
    }

    fun getPositions(): List<Position> {
        return repository.getPositions()
    }

    fun getTrades(): List<Trade> {
        return repository.getTrades()
    }

    fun calculateBrokerage(tradeValue: Double): Double {
        return tradeValue * BROKERAGE_RATE
    }

    fun calculateTotalCharge(tradeValue: Double): Double {
        val brokerage = calculateBrokerage(tradeValue)
        return brokerage * (1.0 + EXTRA_EXPENSE_RATE)
    }

    fun buy(
        symbol: String,
        quantity: Int,
        price: Double
    ): Result<Trade> {

        if (symbol.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Stock symbol is required.")
            )
        }

        if (quantity <= 0) {
            return Result.failure(
                IllegalArgumentException("Quantity must be greater than zero.")
            )
        }

        if (price <= 0.0) {
            return Result.failure(
                IllegalArgumentException("Price must be greater than zero.")
            )
        }

        val tradeValue = quantity * price
        val charge = calculateTotalCharge(tradeValue)
        val totalCost = tradeValue + charge

        if (totalCost > availableCash) {
            return Result.failure(
                IllegalStateException("Insufficient paper-trading cash.")
            )
        }

        availableCash -= totalCost

        val existing = repository
            .getPositions()
            .firstOrNull { it.symbol == symbol }

        val newPosition = if (existing == null) {

            Position(
                symbol = symbol,
                quantity = quantity,
                averagePrice = price,
                lastPrice = price
            )

        } else {

            val newQuantity = existing.quantity + quantity

            val newAveragePrice =
                (
                    (existing.quantity * existing.averagePrice) +
                    (quantity * price)
                ) / newQuantity

            existing.copy(
                quantity = newQuantity,
                averagePrice = newAveragePrice,
                lastPrice = price
            )
        }

        repository.addPosition(newPosition)

        val trade = Trade(
            id = System.currentTimeMillis(),
            symbol = symbol,
            side = TradeSide.BUY,
            quantity = quantity,
            price = price,
            charge = charge,
            timestamp = System.currentTimeMillis()
        )

        repository.addTrade(trade)

        return Result.success(trade)
    }

    fun sell(
        symbol: String,
        quantity: Int,
        price: Double
    ): Result<Trade> {

        if (symbol.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Stock symbol is required.")
            )
        }

        if (quantity <= 0) {
            return Result.failure(
                IllegalArgumentException("Quantity must be greater than zero.")
            )
        }

        if (price <= 0.0) {
            return Result.failure(
                IllegalArgumentException("Price must be greater than zero.")
            )
        }

        val existing = repository
            .getPositions()
            .firstOrNull { it.symbol == symbol }

        if (existing == null) {
            return Result.failure(
                IllegalStateException("No open position for $symbol.")
            )
        }

        if (existing.quantity < quantity) {
            return Result.failure(
                IllegalStateException("Not enough shares to sell.")
            )
        }

        val tradeValue = quantity * price
        val charge = calculateTotalCharge(tradeValue)
        val proceeds = tradeValue - charge

        availableCash += proceeds

        val remainingQuantity = existing.quantity - quantity

        if (remainingQuantity == 0) {

            repository.removePosition(symbol)

        } else {

            repository.addPosition(
                existing.copy(
                    quantity = remainingQuantity,
                    lastPrice = price
                )
            )
        }

        val trade = Trade(
            id = System.currentTimeMillis(),
            symbol = symbol,
            side = TradeSide.SELL,
            quantity = quantity,
            price = price,
            charge = charge,
            timestamp = System.currentTimeMillis()
        )

        repository.addTrade(trade)

        return Result.success(trade)
    }

    fun updateMarketPrice(
        symbol: String,
        price: Double
    ) {

        if (symbol.isBlank() || price <= 0.0) {
            return
        }

        val existing = repository
            .getPositions()
            .firstOrNull { it.symbol == symbol }

        if (existing != null) {

            repository.addPosition(
                existing.copy(
                    lastPrice = price
                )
            )
        }
    }

    fun getUnrealizedPnl(): Double {
        return repository
            .getPositions()
            .sumOf { it.unrealizedPnl }
    }

    fun getInvestedValue(): Double {
        return repository
            .getPositions()
            .sumOf { it.investedValue }
    }

    fun getCurrentValue(): Double {
        return repository
            .getPositions()
            .sumOf { it.currentValue }
    }

    fun getPortfolioSnapshot(): PortfolioSnapshot {

        val investedValue = getInvestedValue()
        val currentValue = getCurrentValue()
        val unrealizedPnl = getUnrealizedPnl()

        return PortfolioSnapshot(
            availableCash = availableCash,
            investedValue = investedValue,
            currentValue = currentValue,
            unrealizedPnl = unrealizedPnl,
            totalValue = availableCash + currentValue
        )
    }
}
