package com.nsepapertrade

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.nsepapertrade.data.InstrumentRepository
import com.nsepapertrade.data.ManualMarketDataProvider
import com.nsepapertrade.data.MarketDataRepository
import com.nsepapertrade.data.PaperTradeEngine
import com.nsepapertrade.data.PaperTradeState
import com.nsepapertrade.ui.PaperTradeScreen

@Composable
fun PaperTradeApp() {

    val scope = rememberCoroutineScope()

    val engine = remember {
        PaperTradeEngine()
    }

    val marketDataProvider = remember {
        ManualMarketDataProvider()
    }

    val marketDataRepository = remember {
        MarketDataRepository(
            provider = marketDataProvider
        )
    }

    val state = remember {
        PaperTradeState(
            engine = engine,
            marketDataRepository = marketDataRepository,
            scope = scope
        )
    }

    val instrumentRepository = remember {
        InstrumentRepository()
    }

    val instruments = remember {
        instrumentRepository.getEquities()
    }

    PaperTradeScreen(
        snapshot = state.snapshot,
        message = state.message,
        instruments = instruments,
        selectedInstrument = state.selectedInstrument,
        quote = state.quote,

        onInstrumentSelected = { instrument ->
            state.selectInstrument(instrument)
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
