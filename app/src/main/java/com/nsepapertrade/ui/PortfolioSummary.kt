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

@Composable
fun PortfolioSummary(
    snapshot: PortfolioSnapshot,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
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

        Text(
            text = "₹${"%,.2f".format(value)}"
        )
    }
}
