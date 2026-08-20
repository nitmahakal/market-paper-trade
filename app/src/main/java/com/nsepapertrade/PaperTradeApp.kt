package com.nsepapertrade

import androidx.compose.runtime.*
import com.nsepapertrade.data.PaperTradeEngine
import com.nsepapertrade.ui.PaperTradeScreen

@Composable
fun PaperTradeApp() {
    val engine = remember { PaperTradeEngine() }

    var cash by remember {
        mutableStateOf(engine.getAvailableCash())
    }

    var message by remember {
        mutableStateOf("")
    }

    PaperTradeScreen(
        cash = cash,
        message = message,
        onBuy = { symbol, quantity, price ->
            val result = engine.buy(
                symbol = symbol,
                quantity = quantity,
                price = price
            )

            if (result.isSuccess) {
                cash = engine.getAvailableCash()
                message = "BUY successful: $symbol × $quantity"
            } else {
                message = result.exceptionOrNull()?.message
                    ?: "BUY failed"
            }
        },
        onSell = { symbol, quantity, price ->
            val result = engine.sell(
                symbol = symbol,
                quantity = quantity,
                price = price
            )

            if (result.isSuccess) {
                cash = engine.getAvailableCash()
                message = "SELL successful: $symbol × $quantity"
            } else {
                message = result.exceptionOrNull()?.message
                    ?: "SELL failed"
            }
        }
    )
}
