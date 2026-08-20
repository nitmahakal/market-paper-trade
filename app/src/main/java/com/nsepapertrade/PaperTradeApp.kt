package com.nsepapertrade

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.nsepapertrade.data.PaperTradeEngine
import com.nsepapertrade.data.PaperTradeState
import com.nsepapertrade.ui.PaperTradeScreen

@Composable
fun PaperTradeApp() {

    val engine = remember {
        PaperTradeEngine()
    }

    val state = remember {
        PaperTradeState(engine)
    }

    PaperTradeScreen(
        snapshot = state.snapshot,
        message = state.message,

        onBuy = { symbol, quantity, price ->
            state.buy(
                symbol = symbol,
                quantity = quantity,
                price = price
            )
        },

        onSell = { symbol, quantity, price ->
            state.sell(
                symbol = symbol,
                quantity = quantity,
                price = price
            )
        }
    )
}
