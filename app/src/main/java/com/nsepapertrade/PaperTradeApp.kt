package com.nsepapertrade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.FnoOptionChainState
import com.nsepapertrade.data.InstrumentRepository
import com.nsepapertrade.data.MarketDataState
import com.nsepapertrade.data.NseFnoMarketDataProvider
import com.nsepapertrade.data.PaperTradeEngine
import com.nsepapertrade.data.PaperTradeState
import com.nsepapertrade.data.YahooMarketDataProvider
import com.nsepapertrade.ui.PaperTradeScreen

@Composable
fun PaperTradeApp() {

    var selectedSection by remember {
        mutableStateOf("EQUITY")
    }

    val marketDataScope = rememberCoroutineScope()

    val marketDataState = remember {
        MarketDataState(
            provider = YahooMarketDataProvider(),
            scope = marketDataScope
        )
    }

    val fnoMarketDataProvider = remember {
        NseFnoMarketDataProvider()
    }

    val fnoOptionChainState = remember {
        FnoOptionChainState(
            provider = fnoMarketDataProvider,
            scope = marketDataScope
        )
    }

    val context = LocalContext.current

    val engine = remember(context) {
        PaperTradeEngine(context)
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

    val instrumentRepository = remember(context) {
        InstrumentRepository(context)
    }

    val instruments = remember(instrumentRepository) {
        instrumentRepository.getEquities()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (selectedSection == "EQUITY") {

                Button(
                    onClick = {
                        selectedSection = "EQUITY"
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Trade Equity")
                }

            } else {

                OutlinedButton(
                    onClick = {
                        selectedSection = "EQUITY"
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Trade Equity")
                }
            }

            if (selectedSection == "FNO") {

                Button(
                    onClick = {
                        selectedSection = "FNO"
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Trade F&O")
                }

            } else {

                OutlinedButton(
                    onClick = {
                        selectedSection = "FNO"
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Trade F&O")
                }
            }
        }

        if (selectedSection == "EQUITY") {

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

        } else {

            FnoHomePlaceholder()
        }
    }
}

@Composable
private fun FnoHomePlaceholder() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {

        Text(
            text = "Trade F&O",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Futures and Options",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = "F&O trading screen will be connected next.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
