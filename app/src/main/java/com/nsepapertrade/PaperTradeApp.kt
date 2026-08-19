package com.nsepapertrade

import androidx.compose.runtime.Composable
import com.nsepapertrade.ui.PaperTradeScreen

@Composable
fun PaperTradeApp() {
    PaperTradeScreen(
        onBuy = { _, _ ->
            // Trading engine connection will be added next.
        },
        onSell = { _, _ ->
            // Trading engine connection will be added next.
        }
    )
}
