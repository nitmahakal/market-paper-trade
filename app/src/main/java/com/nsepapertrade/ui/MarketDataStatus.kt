package com.nsepapertrade.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.MarketDataSource
import com.nsepapertrade.data.MarketQuote

@Composable
fun MarketDataStatus(
    quote: MarketQuote?
) {
    if (quote == null) {
        Text(
            text = "Market data unavailable",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "₹${"%,.2f".format(quote.price)}",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = when (quote.source) {
                MarketDataSource.LIVE -> "LIVE"
                MarketDataSource.DELAYED -> "DELAYED"
                MarketDataSource.MANUAL -> "MANUAL"
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}
