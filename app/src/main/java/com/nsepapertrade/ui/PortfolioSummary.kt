package com.nsepapertrade.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.PortfolioSnapshot
import com.nsepapertrade.model.Position

@Composable
fun PortfolioSummary(
    snapshot: PortfolioSnapshot,
    positions: List<Position> = emptyList(),
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
                    label = "Realized P&L",
                    value = snapshot.realizedPnl
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

        val openPositions = positions
            .filter { it.quantity > 0 }
            .sortedBy { it.symbol.uppercase() }

        if (openPositions.isNotEmpty()) {
            Text(
                text = "Current Positions",
                style = MaterialTheme.typography.titleLarge
            )

            openPositions.forEach { position ->
                CurrentPositionBlock(position = position)
            }
        }
    }
}

@Composable
private fun CurrentPositionBlock(
    position: Position
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = position.symbol,
                style = MaterialTheme.typography.titleMedium
            )

            SummaryRow(
                label = "Qty",
                value = position.quantity.toDouble(),
                integerValue = true
            )

            SummaryRow(
                label = "Avg Buy Price",
                value = position.averagePrice
            )

            SummaryRow(
                label = "LTP",
                value = position.lastPrice
            )

            SummaryRow(
                label = "Invested Value",
                value = position.investedValue
            )

            SummaryRow(
                label = "Current Value",
                value = position.currentValue
            )

            SummaryRow(
                label = "Unrealized P&L",
                value = position.unrealizedPnl
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: Double,
    integerValue: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)

        Text(
            text = if (integerValue) {
                value.toInt().toString()
            } else {
                "₹${"%,.2f".format(value)}"
            }
        )
    }
}
