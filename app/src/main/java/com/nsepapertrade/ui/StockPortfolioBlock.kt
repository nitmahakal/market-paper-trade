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
import com.nsepapertrade.model.Position
import com.nsepapertrade.model.Trade
import com.nsepapertrade.model.TradeSide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StockPortfolioBlock(
    symbol: String,
    position: Position?,
    trades: List<Trade>,
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
                text = symbol,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Current Position",
                style = MaterialTheme.typography.titleMedium
            )

            if (position != null && position.quantity > 0) {
                PortfolioRow("Qty", position.quantity.toString())
                PortfolioRow("Avg Buy Price", formatMoney(position.averagePrice))
                PortfolioRow("LTP", formatMoney(position.lastPrice))
                PortfolioRow("Invested Value", formatMoney(position.investedValue))
                PortfolioRow("Current Value", formatMoney(position.currentValue))
                PortfolioRow("Unrealized P&L", formatMoney(position.unrealizedPnl))
            } else {
                Text(
                    text = "No open position",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (trades.isNotEmpty()) {
                Text(
                    text = "Trade History",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                trades.forEach { trade ->
                    TradeRow(trade)
                }
            }
        }
    }
}

@Composable
private fun PortfolioRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label)
        Text(text = value)
    }
}

@Composable
private fun TradeRow(
    trade: Trade
) {
    val sideText = when (trade.side) {
        TradeSide.BUY -> "BUY"
        TradeSide.SELL -> "SELL"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$sideText  •  Qty ${trade.quantity}  •  ${formatMoney(trade.price)}",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Charge: ${formatMoney(trade.charge)}  •  ${formatDate(trade.timestamp)}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun formatMoney(value: Double): String {
    return "₹${"%,.2f".format(Locale.US, value)}"
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy, HH:mm",
        Locale.getDefault()
    ).format(Date(timestamp))
}
