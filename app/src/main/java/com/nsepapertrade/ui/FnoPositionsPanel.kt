package com.nsepapertrade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nsepapertrade.data.ExitOrder
import com.nsepapertrade.data.ExitType
import com.nsepapertrade.data.FnoTradeState
import com.nsepapertrade.data.MarketClock
import com.nsepapertrade.data.QueuedOrder
import com.nsepapertrade.model.FnoPosition
import com.nsepapertrade.model.FnoPositionSide
import com.nsepapertrade.ui.theme.lossColor
import com.nsepapertrade.ui.theme.onTradeColor
import com.nsepapertrade.ui.theme.profitColor
import java.util.Locale

private const val CONTRACT_WEIGHT = 1.7f
private const val LOTS_WEIGHT = 0.7f
private const val LTP_WEIGHT = 1.1f
private const val PNL_WEIGHT = 1.2f

@Composable
fun FnoPositionsPanel(
    state: FnoTradeState,
    modifier: Modifier = Modifier
) {
    var summaryExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var expandedSymbol by remember {
        mutableStateOf<String?>(null)
    }

    val snapshot = state.snapshot
    val positions = state.positions
    val queued = state.queuedOrders
    val activity = state.activity

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            // ---- Summary (collapsed: only available cash) ----

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        summaryExpanded = !summaryExpanded
                    }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Text(
                        text = "F&O Available Cash",
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
                    text = if (summaryExpanded) "▴" else "▾",
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            if (summaryExpanded) {

                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryRow(
                        "Reserved Margin",
                        snapshot.reservedMargin
                    )

                    SummaryRow(
                        "Realized P&L",
                        snapshot.realizedPnl,
                        colored = true
                    )

                    SummaryRow(
                        "Unrealized P&L",
                        snapshot.unrealizedPnl,
                        colored = true
                    )

                    SummaryRow(
                        "Total Value",
                        snapshot.totalValue
                    )
                }
            }

            // ---- Warnings ----

            if (!MarketClock.isOpen()) {
                Text(
                    text = "Market is closed. New orders are queued " +
                        "and execute at the open price " +
                        "(next open: ${MarketClock.nextOpenLabel()}). " +
                        "Target / stop-loss work only in market hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            if (state.dataError.isNotBlank()) {
                Text(
                    text = "Price feed: ${state.dataError}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // ---- Positions ----

            Text(
                text = "Open positions",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp)
            )

            if (positions.isEmpty()) {

                Text(
                    text = "No open F&O positions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

            } else {

                Text(
                    text = "Tap a position to set target / stop-loss",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeaderCell("Contract", CONTRACT_WEIGHT, TextAlign.Start)
                    HeaderCell("Lots", LOTS_WEIGHT, TextAlign.End)
                    HeaderCell("LTP", LTP_WEIGHT, TextAlign.End)
                    HeaderCell("P&L", PNL_WEIGHT, TextAlign.End)
                }

                positions.forEach { position ->

                    val symbol = position.contract.symbol

                    key(symbol + position.side.name) {

                        PositionRow(
                            position = position,
                            exitCount = state.exitOrders.count {
                                it.contract.symbol.equals(
                                    symbol,
                                    ignoreCase = true
                                )
                            },
                            onClick = {
                                expandedSymbol =
                                    if (expandedSymbol == symbol) {
                                        null
                                    } else {
                                        symbol
                                    }
                            }
                        )

                        if (expandedSymbol == symbol) {
                            ExitOrderEditor(
                                state = state,
                                position = position
                            )
                        }
                    }
                }
            }

            // ---- Queued orders ----

            if (queued.isNotEmpty()) {

                Text(
                    text = "Queued orders (execute at open price)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp)
                )

                queued.forEach { order ->
                    QueuedRow(
                        order = order,
                        onCancel = {
                            state.cancelQueuedOrder(order.id)
                        }
                    )
                }
            }

            // ---- Activity ----

            if (activity.isNotEmpty()) {

                Text(
                    text = "Recent activity",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp)
                )

                activity.take(5).forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PositionRow(
    position: FnoPosition,
    exitCount: Int,
    onClick: () -> Unit
) {
    val pnl = position.unrealizedPnl

    val pnlColor = when {
        pnl > 0.0 -> profitColor()
        pnl < 0.0 -> lossColor()
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {

        Column(
            modifier = Modifier.weight(CONTRACT_WEIGHT)
        ) {
            Text(
                text = position.contract.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "${position.side.name} · Avg " +
                    money(position.averagePrice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (exitCount > 0) {
                Text(
                    text = "Exit orders: $exitCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        BodyCell(position.lots.toString(), LOTS_WEIGHT)

        BodyCell(money(position.lastPrice), LTP_WEIGHT)

        BodyCell(signedMoney(pnl), PNL_WEIGHT, pnlColor)
    }
}

@Composable
private fun ExitOrderEditor(
    state: FnoTradeState,
    position: FnoPosition
) {
    val orders = state.exitOrders.filter {
        it.contract.symbol.equals(
            position.contract.symbol,
            ignoreCase = true
        )
    }

    var type by remember {
        mutableStateOf(ExitType.TARGET)
    }

    var lotsText by remember {
        mutableStateOf(position.lots.toString())
    }

    var triggerText by remember {
        mutableStateOf("")
    }

    var resultText by remember {
        mutableStateOf("")
    }

    var resultOk by remember {
        mutableStateOf(false)
    }

    val mustBeAbove =
        (position.side == FnoPositionSide.LONG &&
            type == ExitType.TARGET) ||
            (position.side == FnoPositionSide.SHORT &&
                type == ExitType.STOP_LOSS)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "Target / Stop-loss",
            style = MaterialTheme.typography.titleSmall
        )

        orders.forEach { order ->
            ExitOrderRow(
                order = order,
                onCancel = { state.cancelExitOrder(order.id) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TypeButton(
                label = "Target",
                selected = type == ExitType.TARGET,
                color = profitColor(),
                modifier = Modifier.weight(1f),
                onClick = { type = ExitType.TARGET }
            )

            TypeButton(
                label = "Stop-loss",
                selected = type == ExitType.STOP_LOSS,
                color = lossColor(),
                modifier = Modifier.weight(1f),
                onClick = { type = ExitType.STOP_LOSS }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = lotsText,
                onValueChange = { text ->
                    lotsText = text.filter { it.isDigit() }
                },
                modifier = Modifier.weight(1f),
                label = { Text("Lots") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            OutlinedTextField(
                value = triggerText,
                onValueChange = { text ->
                    triggerText = text.filter {
                        it.isDigit() || it == '.'
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text("Trigger price") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                )
            )
        }

        Text(
            text = "Position: ${position.side.name} " +
                "${position.lots} lots" +
                (if (position.lastPrice > 0.0) {
                    ", LTP ₹${money(position.lastPrice)}. " +
                        "Trigger must be " +
                        (if (mustBeAbove) "above" else "below") +
                        " LTP."
                } else {
                    "."
                }),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {
                val result = state.addExitOrder(
                    contract = position.contract,
                    type = type,
                    lots = lotsText.toIntOrNull() ?: 0,
                    triggerPrice = triggerText.toDoubleOrNull() ?: 0.0
                )

                resultText = result.message
                resultOk = result.success

                if (result.success) {
                    triggerText = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SET")
        }

        if (resultText.isNotBlank()) {
            Text(
                text = resultText,
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (resultOk) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
            )
        }
    }
}

@Composable
private fun ExitOrderRow(
    order: ExitOrder,
    onCancel: () -> Unit
) {
    val isTarget = order.type == ExitType.TARGET

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = (if (isTarget) "Target" else "Stop-loss") +
                "  ${order.lots} lots @ ₹" +
                money(order.triggerPrice),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isTarget) profitColor() else lossColor()
        )

        Text(
            text = "✕",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable(onClick = onCancel)
                .padding(8.dp)
        )
    }
}

@Composable
private fun QueuedRow(
    order: QueuedOrder,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${order.side.name}  " +
                (order.contract?.displayName ?: order.symbol) +
                " × ${order.quantity} lots",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "✕",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable(onClick = onCancel)
                .padding(8.dp)
        )
    }
}

@Composable
private fun TypeButton(
    label: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = onTradeColor()
            )
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(label)
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
