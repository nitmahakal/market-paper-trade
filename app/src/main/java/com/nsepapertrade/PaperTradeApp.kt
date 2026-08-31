package com.nsepapertrade

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.nsepapertrade.data.InstrumentRepository
import com.nsepapertrade.data.PaperTradeEngine
import com.nsepapertrade.data.PaperTradeState
import com.nsepapertrade.ui.PaperTradeScreen
import com.nsepapertrade.data.MarketDataState
import com.nsepapertrade.data.YahooMarketDataProvider

@Composable
fun PaperTradeApp() {

    val engine = remember {
        PaperTradeEngine()
    }

    val state = remember {
        PaperTradeState(engine)
    }

    val instrumentRepository = remember {
        InstrumentRepository()
    }

    val marketDataState = remember {
        MarketDataState(
        provider = YahooMarketDataProvider(),
        scope = kotlinx.coroutines.rememberCoroutineScope()
        )
    }

    val instruments = remember {
        instrumentRepository.getEquities()
    }

    PaperTradeScreen(
        snapshot = state.snapshot,
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
