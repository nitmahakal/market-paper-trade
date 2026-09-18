package com.nsepapertrade.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.PortfolioSnapshot
import com.nsepapertrade.model.Position
import com.nsepapertrade.model.Trade

@Composable
fun PortfolioSummary(
    snapshot: PortfolioSnapshot,
    positions: List<Position> = emptyList(),
    trades: List<Trade> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Portfolio",
                    style = MaterialTheme.typography.titleMedium
                )

                SummaryRow(
                    label = "Available Cash",
                    value = snapshot.availableCash
                )

                SummaryRow(
                    label = "Invested Value",
                    value = snapshot.investedValue
                )

                SummaryRow(
                    label = "Current Value",
                    value = snapshot.currentValue
                )

                SummaryRow(
                    label = "Unrealized P&L",
                    value = snapshot.unrealizedPnl
                )

                SummaryRow(
                    label = "Total Value",
                    value = snapshot.totalValue
                )
            }
        }

        val symbols = (positions.map { it.symbol } + trades.map { it.symbol })
            .distinctBy { it.uppercase() }
            .sortedBy { it.uppercase() }

        if (symbols.isNotEmpty()) {
            Text(
                text = "Stocks",
                style = MaterialTheme.typography.titleLarge
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = symbols,
                    key = { it.uppercase() }
                ) { symbol ->

                    val position = positions.firstOrNull {
                        it.symbol.equals(symbol, ignoreCase = true)
                    }

                    val stockTrades = trades.filter {
                        it.symbol.equals(symbol, ignoreCase = true)
                    }

                    StockPortfolioBlock(
                        symbol = symbol,
                        position = position,
                        trades = stockTrades
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        Text(text = "₹${"%,.2f".format(value)}")
    }
}
