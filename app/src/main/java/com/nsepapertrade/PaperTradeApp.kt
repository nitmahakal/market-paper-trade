package com.nsepapertrade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.FnoContractStore
import com.nsepapertrade.data.FnoTradeEngine
import com.nsepapertrade.data.InstrumentRepository
import com.nsepapertrade.data.MarketDataState
import com.nsepapertrade.data.MarketQuote
import com.nsepapertrade.data.NseFnoMarketDataProvider
import com.nsepapertrade.data.PaperTradeEngine
import com.nsepapertrade.data.PaperTradeState
import com.nsepapertrade.data.YahooMarketDataProvider
import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.ui.PaperTradeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun PaperTradeApp() {

    var selectedSection by remember {
        mutableStateOf("EQUITY")
    }

    var selectedFnoSection by remember {
        mutableStateOf("FUTURES")
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

    val fnoContracts = remember(context) {
        FnoContractStore(context)
            .loadContracts()
    }

    val futureContracts = remember(fnoContracts) {
        fnoContracts.filter {
            it.contractType ==
                FnoContractType.FUTURE
        }
    }

    val optionCount = remember(fnoContracts) {
        fnoContracts.count {
            it.contractType ==
                FnoContractType.OPTION
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
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
                selectedInstrument =
                    state.selectedInstrument,

                onInstrumentSelected = {
                    instrument ->

                    if (instrument == null) {
                        state.clearSelectedInstrument()
                    } else {
                        state.selectInstrument(
                            instrument
                        )
                    }
                },

                onBuy = {
                    symbol,
                    quantity,
                    price ->

                    state.buy(
                        symbol = symbol,
                        quantity = quantity,
                        price = price
                    )
                },

                onSell = {
                    symbol,
                    quantity,
                    price ->

                    state.sell(
                        symbol = symbol,
                        quantity = quantity,
                        price = price
                    )
                }
            )

        } else {

            FnoHomePlaceholder(
                selectedFnoSection =
                    selectedFnoSection,

                futureContracts =
                    futureContracts,

                optionCount =
                    optionCount,

                onFuturesSelected = {
                    selectedFnoSection = "FUTURES"
                },

                onOptionsSelected = {
                    selectedFnoSection = "OPTIONS"
                },

                provider =
                    fnoMarketDataProvider
            )
        }
    }
}

@Composable
private fun FnoHomePlaceholder(
    selectedFnoSection: String,
    futureContracts: List<FnoContract>,
    optionCount: Int,
    onFuturesSelected: () -> Unit,
    onOptionsSelected: () -> Unit,
    provider: NseFnoMarketDataProvider
) {

    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedFuture by remember {
        mutableStateOf<FnoContract?>(null)
    }

    var lotsText by remember {
        mutableStateOf("1")
    }

    var marketQuote by remember {
        mutableStateOf<MarketQuote?>(null)
    }


    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var tradeMessage by remember {
        mutableStateOf("")
    }

    val context = LocalContext.current

    val fnoTradeEngine = remember(context) {
        FnoTradeEngine(context)
    }

    val filteredFutures = remember(
        futureContracts,
        searchQuery
    ) {

        val query =
            searchQuery.trim()

        if (query.isBlank()) {
            emptyList()
        } else {
            futureContracts.filter {
                it.underlying.contains(
                    query,
                    ignoreCase = true
                )
            }
        }
    }

    LaunchedEffect(selectedFuture) {

        marketQuote = null
        errorMessage = ""
        tradeMessage = ""

        val contract =
            selectedFuture
                ?: return@LaunchedEffect

        while (isActive) {

            isLoading = true

            try {

                val result =
                    withContext(Dispatchers.IO) {
                        provider.getQuote(contract)
                    }

                marketQuote = result

                if (result == null) {
                    errorMessage =
                        "No market data available."
                } else {
                    errorMessage = ""

                    fnoTradeEngine
                        .updateMarketPrice(
                            contractSymbol =
                                contract.symbol,
                            price =
                                result.price
                        )
                }

            } catch (e: Exception) {

                errorMessage =
                    e.message
                        ?: "F&O market data error."

            } finally {

                isLoading = false
            }

            delay(5000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {

        Text(
            text = "Trade F&O",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            if (selectedFnoSection == "FUTURES") {

                Button(
                    onClick =
                        onFuturesSelected,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Futures")
                }

            } else {

                OutlinedButton(
                    onClick =
                        onFuturesSelected,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Futures")
                }
            }

            if (selectedFnoSection == "OPTIONS") {

                Button(
                    onClick =
                        onOptionsSelected,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Options")
                }

            } else {

                OutlinedButton(
                    onClick =
                        onOptionsSelected,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("Options")
                }
            }
        }

        if (selectedFnoSection == "FUTURES") {

            OutlinedTextField(
                value = searchQuery,

                onValueChange = {
                    searchQuery = it
                    selectedFuture = null
                    marketQuote = null
                    tradeMessage = ""
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),

                label = {
                    Text("Search Futures")
                },

                placeholder = {
                    Text(
                        "RELIANCE, NIFTY, BANKNIFTY..."
                    )
                },

                singleLine = true
            )

            if (searchQuery.isNotBlank()) {

                Text(
                    text =
                        "${filteredFutures.size} futures contracts",

                    style =
                        MaterialTheme.typography.bodyMedium,

                    modifier =
                        Modifier.padding(top = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            max = 220.dp
                        )
                        .padding(top = 8.dp)
                ) {

                    items(
                        items = filteredFutures,
                        key = {
                            it.symbol
                        }
                    ) { contract ->

                        OutlinedButton(
                            onClick = {
                                selectedFuture =
                                    contract
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = 4.dp
                                    )
                        ) {

                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    text =
                                        contract.displayName
                                )

                                Text(
                                    text =
                                        "Expiry: ${contract.expiry}  •  Lot: ${contract.lotSize}",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }
                        }
                    }
                }
            }

            selectedFuture?.let { contract ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {

                    Text(
                        text =
                            "Selected Contract",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    Text(
                        text =
                            contract.displayName,

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge,

                        modifier =
                            Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text =
                            "Underlying: ${contract.underlying}",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )

                    Text(
                        text =
                            "Expiry: ${contract.expiry}",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )

                    Text(
                        text =
                            "Lot Size: ${contract.lotSize}",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )

                    if (isLoading) {

                        Text(
                            text =
                                "Updating LTP...",

                            modifier =
                                Modifier.padding(top = 8.dp)
                        )
                    }

                    marketQuote?.let { quote ->

                        Text(
                            text =
                                "LTP: ${
                                    "%.2f"
                                        .format(
                                            quote.price
                                        )
                                }",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,

                            modifier =
                                Modifier.padding(top = 8.dp)
                        )
                    }

                    if (errorMessage.isNotBlank()) {

                        Text(
                            text = errorMessage,

                            modifier =
                                Modifier.padding(
                                    top = 8.dp
                                )
                        )
                    }

                    OutlinedTextField(
                        value = lotsText,

                        onValueChange = {
                            lotsText =
                                it.filter {
                                    character ->
                                    character.isDigit()
                                }
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),

                        label = {
                            Text("Lots")
                        },

                        singleLine = true
                    )

                    val lots =
                        lotsText.toIntOrNull()
                            ?: 0

                    val price =
                        marketQuote?.price
                            ?: 0.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),

                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Button(
                            onClick = {

                                if (
                                    price > 0.0 &&
                                    lots > 0
                                ) {

                                    val result =
                                        fnoTradeEngine.buy(
                                            contract =
                                                contract,
                                            lots =
                                                lots,
                                            price =
                                                price
                                        )

                                    tradeMessage =
                                        if (
                                            result.isSuccess
                                        ) {
                                            "BUY successful"
                                        } else {
                                            result
                                                .exceptionOrNull()
                                                ?.message
                                                ?: "BUY failed"
                                        }
                                }
                            },

                            modifier =
                                Modifier.weight(1f),

                            enabled =
                                price > 0.0 &&
                                    lots > 0
                        ) {
                            Text("BUY")
                        }

                        OutlinedButton(
                            onClick = {

                                if (
                                    price > 0.0 &&
                                    lots > 0
                                ) {

                                    val result =
                                        fnoTradeEngine.sell(
                                            contract =
                                                contract,
                                            lots =
                                                lots,
                                            price =
                                                price
                                        )

                                    tradeMessage =
                                        if (
                                            result.isSuccess
                                        ) {
                                            "SELL successful"
                                        } else {
                                            result
                                                .exceptionOrNull()
                                                ?.message
                                                ?: "SELL failed"
                                        }
                                }
                            },

                            modifier =
                                Modifier.weight(1f),

                            enabled =
                                price > 0.0 &&
                                    lots > 0
                        ) {
                            Text("SELL")
                        }
                    }

                    if (tradeMessage.isNotBlank()) {

                        Text(
                            text =
                                tradeMessage,

                            modifier =
                                Modifier.padding(
                                    top = 8.dp
                                )
                        )
                    }
                }
            }

        } else {

            Text(
                text = "Options",

                style =
                    MaterialTheme
                        .typography
                        .titleLarge,

                modifier =
                    Modifier.padding(top = 20.dp)
            )

            Text(
                text =
                    "Option chain will be connected next.",

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                modifier =
                    Modifier.padding(top = 8.dp)
            )

            Text(
                text =
                    "Stored option contracts: $optionCount",

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                modifier =
                    Modifier.padding(top = 8.dp)
            )
        }
    }
}
