package com.nsepapertrade

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.nsepapertrade.data.InstrumentRepository
import com.nsepapertrade.data.MarketDataState
import com.nsepapertrade.data.PaperTradeEngine
import com.nsepapertrade.data.PaperTradeState
import com.nsepapertrade.data.YahooMarketDataProvider
import com.nsepapertrade.ui.PaperTradeScreen

@Composable
fun PaperTradeApp() {
    val marketDataScope = rememberCoroutineScope()

    val marketDataState = remember {
        MarketDataState(
            provider = YahooMarketDataProvider(),
            scope = marketDataScope
        )
    }

    val engine = remember {
        PaperTradeEngine()
    }

    val state = remember {
        PaperTradeState(engine)
    }

    LaunchedEffect(state.selectedInstrument) {
        val instrument = state.selectedInstrument

        if (instrument != null) {
            marketDataState.start(instrument)
        } else {
            marketDataState.stop()
        }
    }

    val marketQuote = marketDataState.quote

    val context = LocalContext.current

    val instrumentRepository = remember(context) {
        InstrumentRepository(context)
    }

    val instruments = remember(instrumentRepository) {
        instrumentRepository.getEquities()
    }

    PaperTradeScreen(
        snapshot = state.snapshot,
        positions = state.positions,
        marketQuote = marketQuote,
        message = state.message,
        instruments = instruments,
        selectedInstrument = state.selectedInstrument,

        onInstrumentSelected = { instrument ->
            if (instrument == null) {
                state.clearSelectedInstrument()
            } else {
                state.selectInstrument(instrument)
            }
        },

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
