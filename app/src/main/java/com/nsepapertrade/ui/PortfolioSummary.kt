package com.nsepapertrade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nsepapertrade.data.PortfolioSnapshot
import com.nsepapertrade.model.Position
import com.nsepapertrade.ui.theme.lossColor
import com.nsepapertrade.ui.theme.profitColor
import java.util.Locale

private const val SYMBOL_WEIGHT = 1.5f
private const val QTY_WEIGHT = 0.8f
private const val LTP_WEIGHT = 1.2f
private const val PNL_WEIGHT = 1.3f

@Composable
fun PortfolioSummary(
    snapshot: PortfolioSnapshot,
    positions: List<Position> = emptyList(),
    onPositionClick: (Position) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        SummaryCard(snapshot = snapshot)

        val openPositions = positions
            .filter { it.quantity > 0 }
            .sortedBy { it.symbol.uppercase() }

        if (openPositions.isNotEmpty()) {
            PositionsTable(
                positions = openPositions,
                onPositionClick = onPositionClick
            )
        }
    }
}

@Composable
private fun SummaryCard(
    snapshot: PortfolioSnapshot
) {
    var expanded by rememberSaveable {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Text(
                        text = "Available Cash",
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "₹${money(snapshot.availableCash)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Text(
                    text = if (expanded) "▴" else "▾",
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (expanded) {

                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

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
                        value = snapshot.realizedPnl,
                        colored = true
                    )

                    SummaryRow(
                        label = "Unrealized P&L",
                        value = snapshot.unrealizedPnl,
                        colored = true
                    )

                    SummaryRow(
                        label = "Total Value",
                        value = snapshot.totalValue
                    )
                }
            }
        }
    }
}

@Composable
private fun PositionsTable(
    positions: List<Position>,
    onPositionClick: (Position) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {

        Text(
            text = "Positions",
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = "Tap a symbol to trade",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderCell("Symbol", SYMBOL_WEIGHT, TextAlign.Start)
            HeaderCell("Qty", QTY_WEIGHT, TextAlign.End)
            HeaderCell("LTP", LTP_WEIGHT, TextAlign.End)
            HeaderCell("P&L", PNL_WEIGHT, TextAlign.End)
        }

        positions.forEach { position ->

            val pnl = position.unrealizedPnl

            val pnlColor = when {
                pnl > 0.0 -> profitColor()
                pnl < 0.0 -> lossColor()
                else -> MaterialTheme.colorScheme.onSurface
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPositionClick(position) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {

                Column(
                    modifier = Modifier.weight(SYMBOL_WEIGHT)
                ) {
                    Text(
                        text = position.symbol,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Avg ${money(position.averagePrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                BodyCell(
                    text = position.quantity.toString(),
                    weight = QTY_WEIGHT
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
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align
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
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = TextAlign.End
    )
}

@Composable
private fun SummaryRow(
    label: String,
    value: Double,
    colored: Boolean = false
) {
    val valueColor = when {
        !colored -> Color.Unspecified
        value > 0.0 -> profitColor()
        value < 0.0 -> lossColor()
        else -> Color.Unspecified
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = if (colored) {
                "₹${signedMoney(value)}"
            } else {
                "₹${money(value)}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor
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
