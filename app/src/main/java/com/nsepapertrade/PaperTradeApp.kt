package com.nsepapertrade

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.nsepapertrade.data.InstrumentRepository
import com.nsepapertrade.data.PaperTradeEngine
import com.nsepapertrade.data.PaperTradeState
import com.nsepapertrade.ui.PaperTradeScreen
import androidx.compose.runtime.rememberCoroutineScope
import com.nsepapertrade.data.MarketDataState
import com.nsepapertrade.data.YahooMarketDataProvider
import androidx.compose.runtime.LaunchedEffect

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

    val instrumentRepository = remember {
        InstrumentRepository()
    }
    
  
    val instruments = remember {
        instrumentRepository.getEquities()
    }

    PaperTradeScreen(
        snapshot = state.snapshot,
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
