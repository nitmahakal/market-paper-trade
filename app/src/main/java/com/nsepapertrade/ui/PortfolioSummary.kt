package com.nsepapertrade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.PortfolioSnapshot
import com.nsepapertrade.model.Position
import java.util.Locale

private val ProfitColor = Color(0xFF2E7D32)
private val LossColor = Color(0xFFC62828)

private const val SYMBOL_WEIGHT = 1.5f
private const val QTY_WEIGHT = 0.9f
private const val AVG_WEIGHT = 1.2f
private const val LTP_WEIGHT = 1.2f
private const val PNL_WEIGHT = 1.4f

@Composable
fun PortfolioSummary(
    snapshot: PortfolioSnapshot,
    positions: List<Position> = emptyList(),
    onPositionClick: (Position) -> Unit = {},
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
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Tap a symbol to trade",
                style = MaterialTheme.typography.bodySmall
            )

            PositionsTable(
                positions = openPositions,
                onPositionClick = onPositionClick
            )
        }
    }
}

@Composable
private fun PositionsTable(
    positions: List<Position>,
    onPositionClick: (Position) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCell("Symbol", SYMBOL_WEIGHT, TextAlign.Start)
                HeaderCell("Qty", QTY_WEIGHT, TextAlign.End)
                HeaderCell("Avg", AVG_WEIGHT, TextAlign.End)
                HeaderCell("LTP", LTP_WEIGHT, TextAlign.End)
                HeaderCell("P&L", PNL_WEIGHT, TextAlign.End)
            }

            HorizontalDivider()

            positions.forEach { position ->

                val pnl = position.unrealizedPnl

                val pnlColor = when {
                    pnl > 0.0 -> ProfitColor
                    pnl < 0.0 -> LossColor
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPositionClick(position) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = position.symbol,
                        modifier = Modifier.weight(SYMBOL_WEIGHT),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    BodyCell(
                        text = position.quantity.toString(),
                        weight = QTY_WEIGHT
                    )

                    BodyCell(
                        text = money(position.averagePrice),
                        weight = AVG_WEIGHT
                    )

                    BodyCell(
                        text = money(position.lastPrice),
                        weight = LTP_WEIGHT
                    )

                    BodyCell(
                        text = signedMoney(pnl),
                        weight = PNL_WEIGHT,
                        color = pnlColor
                    )
                }

                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    weight: Float,
    align: TextAlign
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        textAlign = align,
        maxLines = 1
    )
}

@Composable
private fun RowScope.BodyCell(
    text: String,
    weight: Float,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.bodySmall,
        color = color,
        textAlign = TextAlign.End,
        maxLines = 1,
        softWrap = false
    )
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
                "₹${money(value)}"
            }
        )
    }
}

private fun money(value: Double): String {
    return "%,.2f".format(Locale.US, value)
}

private fun signedMoney(value: Double): String {
    return if (value > 0.0) {
        "+${money(value)}"
    } else {
        money(value)
    }
}
