package com.nsepapertrade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.FnoContractManager
import com.nsepapertrade.data.FnoMarginManager
import com.nsepapertrade.data.NseSpanMarginProvider
import com.nsepapertrade.data.FnoContractStore
import com.nsepapertrade.data.NseFnoContractProvider
import com.nsepapertrade.data.FnoTradeState
import com.nsepapertrade.data.MarketClock
import com.nsepapertrade.data.OrderBook
import com.nsepapertrade.data.OrderKind
import com.nsepapertrade.model.TradeSide
import com.nsepapertrade.ui.FnoPositionsPanel
import com.nsepapertrade.data.InstrumentRepository
import com.nsepapertrade.data.MarketDataState
import com.nsepapertrade.data.MarketQuote
import com.nsepapertrade.data.NseFnoMarketDataProvider
import com.nsepapertrade.data.PaperTradeEngine
import com.nsepapertrade.data.PaperTradeState
import com.nsepapertrade.data.YahooMarketDataProvider
import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.data.FnoOptionChain
import com.nsepapertrade.model.OptionType
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

    val equityOrderBook = remember(context) {
        OrderBook(context, OrderKind.EQUITY)
    }

    val state = remember {
        PaperTradeState(
            engine = engine,
            orderBook = equityOrderBook
        )
    }

    val fnoState = remember(context) {
        FnoTradeState(context)
    }

    LaunchedEffect(state.selectedInstrument) {

        val instrument =
            state.selectedInstrument

        if (instrument != null) {
            marketDataState.start(
                instrument
            )
        } else {
            marketDataState.stop()
        }
    }

    val marketQuote =
        marketDataState.quote

    val instrumentRepository =
        remember(context) {
            InstrumentRepository(context)
        }

    val instruments =
        remember(instrumentRepository) {
            instrumentRepository.getEquities()
        }

    val fnoContractManager =
        remember(context) {
            FnoContractManager(
                context = context,
                provider = NseFnoContractProvider()
            )
        }

    val fnoMarginManager =
        remember(context) {
            FnoMarginManager(
                context = context,
                provider = NseSpanMarginProvider()
            )
        }

    var fnoMarginMessage by remember {
        mutableStateOf("")
    }

    var fnoMarginLoading by remember {
        mutableStateOf(false)
    }

    var fnoContracts by remember(context) {
        mutableStateOf(
            FnoContractStore(context)
                .loadContracts()
        )
    }

    var fnoUpdateMessage by remember {
        mutableStateOf("")
    }

    var fnoUpdateLoading by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {

        fnoUpdateLoading = true
        fnoUpdateMessage = "Updating F&O contracts..."

        try {

            val result =
                fnoContractManager.update()

            fnoContracts =
                fnoContractManager.getContracts()

            fnoUpdateMessage =
                if (result.success) {
                    "F&O contracts updated: ${result.contractCount}"
                } else {
                    result.message
                }

            if (result.success) {

                fnoMarginLoading = true
                fnoMarginMessage =
                    "Updating F&O margin data..."

                val marginResult =
                    fnoMarginManager.update()

                fnoMarginMessage =
                    if (marginResult.success) {
                        "F&O margin updated: ${marginResult.marginCount}"
                    } else {
                        marginResult.message
                    }

                fnoMarginLoading = false
            }

        } catch (e: Exception) {

            fnoUpdateMessage =
                e.message
                    ?: "F&O contract update failed."

        } finally {

            fnoUpdateLoading = false
            fnoMarginLoading = false
        }
    }

    val futureContracts =
        remember(fnoContracts) {
            fnoContracts.filter {
                it.contractType ==
                    FnoContractType.FUTURE
            }
        }

    val optionCount =
        remember(fnoContracts) {
            fnoContracts.count {
                it.contractType ==
                    FnoContractType.OPTION
            }
        }

    val imeVisible =
        WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

        if (
            selectedSection ==
                "EQUITY"
        ) {

            PaperTradeScreen(
                snapshot =
                    state.snapshot,
                positions =
                    state.positions,
                marketQuote =
                    marketQuote,
                message =
                    state.message,
                instruments =
                    instruments,
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
                        symbol =
                            symbol,
                        quantity =
                            quantity,
                        price =
                            price
                    )
                },

                onSell = {
                    symbol,
                    quantity,
                    price ->

                    state.sell(
                        symbol =
                            symbol,
                        quantity =
                            quantity,
                        price =
                            price
                    )
                },

                queuedOrders =
                    state.queuedOrders,

                onCancelQueued = {
                    id ->

                    state.cancelQueuedOrder(id)
                }
            )

        } else {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 8.dp,
                        vertical = 12.dp
                    )
            ) {

                if (fnoUpdateLoading ||
                    fnoUpdateMessage.isNotBlank()
                ) {
                    val clipboardManager =
                        LocalClipboardManager.current

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = fnoUpdateMessage,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )

                        if (
                            fnoUpdateMessage.contains(
                                "F&O diagnostic",
                                ignoreCase = true
                            )
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(
                                        AnnotatedString(
                                            fnoUpdateMessage
                                        )
                                    )
                                },
                                modifier = Modifier.padding(
                                    top = 6.dp
                                )
                            ) {
                                Text("COPY DIAGNOSTIC")
                            }
                        }
                    }
                }

                if (fnoMarginLoading ||
                    fnoMarginMessage.isNotBlank()
                ) {
                    Text(
                        text = fnoMarginMessage,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        modifier =
                            Modifier.padding(
                                bottom = 8.dp
                            )
                    )
                }

                FnoHomePlaceholder(
                    selectedFnoSection =
                        selectedFnoSection,

                    futureContracts =
                        futureContracts,

                    optionContracts =
                        fnoContracts.filter {
                            it.contractType ==
                                FnoContractType.OPTION
                        },

                    optionCount =
                        optionCount,

                onFuturesSelected = {
                    selectedFnoSection =
                        "FUTURES"
                },

                onOptionsSelected = {
                    selectedFnoSection =
                        "OPTIONS"
                },

                fnoState =
                    fnoState,

                provider =
                    fnoMarketDataProvider
                )
            }
        }

        }

        if (!imeVisible) {
            BottomTabs(
                selectedSection = selectedSection,
                onSelect = { selectedSection = it }
            )
        }
    }
}

@Composable
private fun BottomTabs(
    selectedSection: String,
    onSelect: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            BottomTab(
                label = "Equity",
                selected = selectedSection == "EQUITY",
                onClick = { onSelect("EQUITY") }
            )

            BottomTab(
                label = "F&O",
                selected = selectedSection == "FNO",
                onClick = { onSelect("FNO") }
            )
        }
    }
}

@Composable
private fun RowScope.BottomTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }
                )
        )

        Text(
            text = label,
            modifier = Modifier.padding(vertical = 14.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight =
                if (selected) FontWeight.Bold else FontWeight.Normal,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
        )
    }
}

@Composable
private fun FnoHomePlaceholder(
    selectedFnoSection: String,
    futureContracts: List<FnoContract>,
    optionContracts: List<FnoContract>,
    optionCount: Int,
    onFuturesSelected: () -> Unit,
    onOptionsSelected: () -> Unit,
    fnoState: FnoTradeState,
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

                    fnoState.updateMarketPrice(
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
            .padding(top = 20.dp)
            .verticalScroll(
                rememberScrollState()
            )
    ) {

        Text(
            text = "Trade F&O",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Futures & Options",
            style =
                MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier.padding(top = 2.dp)
        )

        FnoPositionsPanel(
            state = fnoState,
            modifier = Modifier.padding(top = 16.dp)
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
                    onClick = onFuturesSelected,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("FUTURES")
                }

            } else {

                OutlinedButton(
                    onClick = onFuturesSelected,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("FUTURES")
                }
            }

            if (selectedFnoSection == "OPTIONS") {

                Button(
                    onClick = onOptionsSelected,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("OPTIONS")
                }

            } else {

                OutlinedButton(
                    onClick = onOptionsSelected,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text("OPTIONS")
                }
            }
        }

        if (selectedFnoSection == "FUTURES") {

            Text(
                text = "Select Futures Contract",
                style =
                    MaterialTheme.typography.titleMedium,
                modifier =
                    Modifier.padding(top = 20.dp)
            )

            OutlinedTextField(
                value = searchQuery,

                onValueChange = {
                    searchQuery = it
                    selectedFuture = null
                    marketQuote = null
                    tradeMessage = ""
                    errorMessage = ""
                },

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),

                label = {
                    Text("Search underlying")
                },

                placeholder = {
                    Text(
                        "RELIANCE / NIFTY / BANKNIFTY"
                    )
                },

                singleLine = true
            )

            if (searchQuery.isNotBlank()) {

                Text(
                    text =
                        "${filteredFutures.size} contracts found",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    modifier =
                        Modifier.padding(top = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            max = 220.dp
                        )
                        .padding(top = 6.dp)
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
                                        vertical = 3.dp
                                    )
                        ) {

                            Column(
                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    text =
                                        contract.displayName,

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyLarge
                                )

                                Text(
                                    text =
                                        "Expiry ${contract.expiry}   •   Lot ${contract.lotSize}",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall,

                                    modifier =
                                        Modifier.padding(
                                            top = 2.dp
                                        )
                                )
                            }
                        }
                    }
                }
            }

            selectedFuture?.let { contract ->

                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {

                        Text(
                            text = "SELECTED CONTRACT",
                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium
                        )

                        Text(
                            text =
                                contract.displayName,

                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,

                            modifier =
                                Modifier.padding(top = 4.dp)
                        )

                        Text(
                            text =
                                "Underlying  ${contract.underlying}",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,

                            modifier =
                                Modifier.padding(top = 10.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    text = "EXPIRY",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall
                                )

                                Text(
                                    text =
                                        contract.expiry,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyLarge
                                )
                            }

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    text = "LOT SIZE",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall
                                )

                                Text(
                                    text =
                                        contract.lotSize
                                            .toString(),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyLarge
                                )
                            }

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    text = "LTP",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall
                                )

                                Text(
                                    text =
                                        marketQuote?.let {
                                            "%.2f".format(
                                                it.price
                                            )
                                        }
                                            ?: "--",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                )
                            }
                        }

                        if (isLoading) {

                            Text(
                                text =
                                    "Updating market price...",

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,

                                modifier =
                                    Modifier.padding(
                                        top = 10.dp
                                    )
                            )
                        }

                        if (
                            errorMessage.isNotBlank()
                        ) {

                            Text(
                                text = errorMessage,

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,

                                modifier =
                                    Modifier.padding(
                                        top = 10.dp
                                    )
                            )
                        }

                        if (!MarketClock.isOpen()) {
                            Text(
                                text = MarketClock.closedNotice(),
                                style =
                                    MaterialTheme.typography.bodySmall,
                                color =
                                    MaterialTheme.colorScheme.error,
                                modifier =
                                    Modifier.padding(top = 10.dp)
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
                                .padding(top = 16.dp),

                            label = {
                                Text("Lots")
                            },

                            placeholder = {
                                Text("Enter number of lots")
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
                                .padding(top = 14.dp),
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {

                            Button(
                                onClick = {

                                    if (
                                    lots > 0 &&
                                        (price > 0.0 || !MarketClock.isOpen())
                                ) {

                                    tradeMessage =
                                        fnoState.placeOrder(
                                            contract = contract,
                                            side = TradeSide.BUY,
                                            lots = lots,
                                            price = price
                                        ).message
                                }
                                },

                                modifier =
                                    Modifier.weight(1f),

                                enabled =
                                    lots > 0 &&
                                        (price > 0.0 || !MarketClock.isOpen())
                            ) {
                                Text("BUY")
                            }

                            OutlinedButton(
                                onClick = {

                                    if (
                                    lots > 0 &&
                                        (price > 0.0 || !MarketClock.isOpen())
                                ) {

                                    tradeMessage =
                                        fnoState.placeOrder(
                                            contract = contract,
                                            side = TradeSide.SELL,
                                            lots = lots,
                                            price = price
                                        ).message
                                }
                                },

                                modifier =
                                    Modifier.weight(1f),

                                enabled =
                                    lots > 0 &&
                                        (price > 0.0 || !MarketClock.isOpen())
                            ) {
                                Text("SELL")
                            }
                        }

                        if (
                            tradeMessage.isNotBlank()
                        ) {

                            Text(
                                text = tradeMessage,

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,

                                modifier =
                                    Modifier.padding(
                                        top = 10.dp
                                    )
                            )
                        }
                    }
                }
            }

         } else {

            var optionSearch by remember {
                mutableStateOf("")
            }

            var selectedOptionUnderlying by remember {
                mutableStateOf("")
            }

            var selectedOptionExpiry by remember {
                mutableStateOf("")
            }

            var optionChain by remember {
                mutableStateOf<FnoOptionChain?>(null)
            }

            var selectedOptionContract by remember {
                mutableStateOf<FnoContract?>(null)
            }

            var optionMarketQuote by remember {
                mutableStateOf<MarketQuote?>(null)
            }

            var optionLotsText by remember {
                mutableStateOf("1")
            }

            var optionTradeMessage by remember {
                mutableStateOf("")
            }

            var optionChainLoading by remember {
                mutableStateOf(false)
            }

            var optionChainMessage by remember {
                mutableStateOf("")
            }

            val availableOptionContracts =
                remember(optionContracts) {
                    optionContracts
                }

            LaunchedEffect(selectedOptionContract) {

                optionMarketQuote = null
                optionTradeMessage = ""

                val contract =
                    selectedOptionContract
                        ?: return@LaunchedEffect

                while (isActive) {

                    try {

                        val result =
                            withContext(Dispatchers.IO) {
                                provider.getQuote(contract)
                            }

                        optionMarketQuote = result

                        if (result != null) {

                            fnoState.updateMarketPrice(
                                contractSymbol =
                                    contract.symbol,
                                price =
                                    result.price
                            )
                        }

                    } catch (e: Exception) {

                        optionMarketQuote = null
                    }

                    delay(5000)
                }
            }

            val matchingUnderlyings =
                remember(
                    availableOptionContracts,
                    optionSearch
                ) {

                    val query =
                        optionSearch
                            .trim()
                            .uppercase()

                    if (query.isBlank()) {
                        emptyList()
                    } else {
                        optionContracts
                            .map {
                                it.underlying
                            }
                            .distinct()
                            .filter {
                                it.contains(
                                    query,
                                    ignoreCase = true
                                )
                            }
                            .sorted()
                    }
                }

            val availableExpiries =
                remember(
                    optionContracts,
                    selectedOptionUnderlying
                ) {

                    optionContracts
                        .filter {
                            it.underlying.equals(
                                selectedOptionUnderlying,
                                ignoreCase = true
                            )
                        }
                        .map {
                            it.expiry
                        }
                        .distinct()
                        .sorted()
                }

            androidx.compose.material3.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {

                Column(
                    modifier =
                        Modifier.padding(12.dp)
                ) {

                    Text(
                        text = "OPTIONS",

                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )

                    Text(
                        text =
                            "Option Chain",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        modifier =
                            Modifier.padding(top = 12.dp)
                    )

                    OutlinedTextField(
                        value =
                            optionSearch,

                        onValueChange = {
                            optionSearch = it
                            selectedOptionUnderlying = ""
                            selectedOptionExpiry = ""
                            optionChain = null
                            selectedOptionContract = null
                            optionChainMessage = ""
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),

                        label = {
                            Text(
                                "Search underlying"
                            )
                        },

                        placeholder = {
                            Text(
                                "NIFTY / BANKNIFTY / RELIANCE"
                            )
                        },

                        singleLine = true
                    )

                    if (
                        matchingUnderlyings
                            .isNotEmpty()
                    ) {

                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(
                                        max = 180.dp
                                    )
                                    .padding(top = 6.dp)
                        ) {

                            items(
                                items =
                                    matchingUnderlyings,
                                key = {
                                    it
                                }
                            ) { underlying ->

                                OutlinedButton(
                                    onClick = {

                                        selectedOptionUnderlying =
                                            underlying

                                        selectedOptionExpiry =
                                            ""

                                        optionChain =
                                            null

                                        selectedOptionContract =
                                            null

                                        optionChainMessage =
                                            ""
                                    },

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                vertical = 3.dp
                                            )
                                ) {
                                    Text(
                                        underlying
                                    )
                                }
                            }
                        }
                    }

                    if (
                        selectedOptionUnderlying
                            .isNotBlank()
                    ) {

                        Text(
                            text =
                                "Underlying: $selectedOptionUnderlying",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyLarge,

                            modifier =
                                Modifier.padding(
                                    top = 12.dp
                                )
                        )

                        Text(
                            text =
                                "Select Expiry",

                            style =
                                MaterialTheme
                                    .typography
                                    .labelLarge,

                            modifier =
                                Modifier.padding(
                                    top = 10.dp
                                )
                        )

                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(
                                        max = 150.dp
                                    )
                                    .padding(top = 4.dp)
                        ) {

                            items(
                                items =
                                    availableExpiries,
                                key = {
                                    it
                                }
                            ) { expiry ->

                                OutlinedButton(
                                    onClick = {

                                        selectedOptionExpiry =
                                            expiry

                                        selectedOptionContract =
                                            null

                                        optionChain =
                                            null

                                        optionChainMessage =
                                            "Loading option chain..."

                                        optionChainLoading =
                                            true
                                    },

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                vertical = 2.dp
                                            )
                                ) {
                                    Text(
                                        expiry
                                    )
                                }
                            }
                        }
                    }

                    LaunchedEffect(
                        selectedOptionUnderlying,
                        selectedOptionExpiry
                    ) {

                        if (
                            selectedOptionUnderlying
                                .isBlank() ||
                            selectedOptionExpiry
                                .isBlank()
                        ) {
                            return@LaunchedEffect
                        }

                        optionChainLoading = true
                        optionChainMessage =
                            "Loading option chain..."

                        try {

                            val result =
                                withContext(
                                    Dispatchers.IO
                                ) {
                                    provider
                                        .getOptionChain(
                                            underlying =
                                                selectedOptionUnderlying,
                                            expiry =
                                                selectedOptionExpiry
                                        )
                                }

                            optionChain =
                                result

                            optionChainMessage =
                                if (result == null) {
                                    "No option chain data available."
                                } else {
                                    ""
                                }

                        } catch (e: Exception) {

                            optionChain =
                                null

                            optionChainMessage =
                                e.message
                                    ?: "Option chain error."

                        } finally {

                            optionChainLoading =
                                false
                        }
                    }

                    if (optionChainLoading) {

                        Text(
                            text =
                                "Loading option chain...",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            modifier =
                                Modifier.padding(
                                    top = 10.dp
                                )
                        )
                    }

                    if (
                        optionChainMessage
                            .isNotBlank()
                    ) {

                        Text(
                            text =
                                optionChainMessage,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            modifier =
                                Modifier.padding(
                                    top = 8.dp
                                )
                        )
                    }

                    optionChain?.let { chain ->

                        Text(
                            text =
                                "Strike        CE LTP        PE LTP",

                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium,

                            modifier =
                                Modifier.padding(
                                    top = 14.dp
                                )
                        )

                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(
                                        max = 320.dp
                                    )
                                    .padding(top = 6.dp)
                        ) {
                            items(
                                items = chain.rows,
                                key = {
                                    it.strikePrice
                                }
                            ) { row ->

                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                vertical = 4.dp
                                            )
                                ) {

                                    Text(
                                        text =
                                            "Strike %.2f"
                                                .format(
                                                    row.strikePrice
                                                ),

                                        style =
                                            MaterialTheme
                                                .typography
                                                .labelMedium,

                                        modifier =
                                            Modifier.padding(
                                                bottom = 4.dp
                                            )
                                    )

                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        horizontalArrangement =
                                            Arrangement.spacedBy(4.dp)
                                    ) {

                                        OutlinedButton(
                                            onClick = {
                                                row.call?.let {
                                                    selectedOptionContract =
                                                        it.contract
                                                }
                                            },

                                            modifier =
                                                Modifier.weight(1f)
                                        ) {

                                            Text(
                                                text =
                                                    row.call
                                                        ?.ltp
                                                        ?.let {
                                                            "CE %.2f"
                                                                .format(it)
                                                        }
                                                        ?: "CE --"
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                row.put?.let {
                                                    selectedOptionContract =
                                                        it.contract
                                                }
                                            },

                                            modifier =
                                                Modifier.weight(1f)
                                        ) {

                                            Text(
                                                text =
                                                    row.put
                                                        ?.ltp
                                                        ?.let {
                                                            "PE %.2f"
                                                                .format(it)
                                                        }
                                                        ?: "PE --"
                                            )
                                        }
                                    }
                                }
                            }
                        }

                            selectedOptionContract?.let {
                                contract ->

                                androidx.compose.material3.Card(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                top = 14.dp
                                            )
                                ) {

                                    Column(
                                        modifier =
                                            Modifier.padding(
                                                12.dp
                                            )
                                    ) {

                                        Text(
                                            text =
                                                "SELECTED CONTRACT",

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .labelMedium
                                        )

                                        Text(
                                            text =
                                                contract.displayName,

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .titleMedium,

                                            modifier =
                                                Modifier.padding(
                                                    top = 4.dp
                                                )
                                        )

                                        Row(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        top = 10.dp
                                                    ),

                                            horizontalArrangement =
                                                Arrangement.spacedBy(8.dp)
                                        ) {

                                            Column(
                                                modifier =
                                                    Modifier.weight(1f)
                                            ) {

                                                Text(
                                                    text =
                                                        "LOT SIZE",

                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .labelSmall
                                                )

                                                Text(
                                                    text =
                                                        contract.lotSize.toString(),

                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .bodyLarge
                                                )
                                            }

                                            Column(
                                                modifier =
                                                    Modifier.weight(1f)
                                            ) {

                                                Text(
                                                    text =
                                                        "LTP",

                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .labelSmall
                                                )

                                                Text(
                                                    text =
                                                        optionMarketQuote
                                                            ?.price
                                                            ?.let {
                                                                "%.2f".format(it)
                                                            }
                                                            ?: "--",

                                                    style =
                                                        MaterialTheme
                                                            .typography
                                                            .titleMedium
                                                )
                                            }
                                        }

                                        if (!MarketClock.isOpen()) {
                            Text(
                                text = MarketClock.closedNotice(),
                                style =
                                    MaterialTheme.typography.bodySmall,
                                color =
                                    MaterialTheme.colorScheme.error,
                                modifier =
                                    Modifier.padding(top = 10.dp)
                            )
                        }

                                        OutlinedTextField(
                                            value =
                                                optionLotsText,

                                            onValueChange = {
                                                optionLotsText =
                                                    it.filter { character ->
                                                        character.isDigit()
                                                    }
                                            },

                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        top = 14.dp
                                                    ),

                                            label = {
                                                Text("Lots")
                                            },

                                            placeholder = {
                                                Text(
                                                    "Enter number of lots"
                                                )
                                            },

                                            singleLine = true
                                        )

                                        val optionLots =
                                            optionLotsText.toIntOrNull()
                                                ?: 0

                                        val optionPrice =
                                            optionMarketQuote
                                                ?.price
                                                ?: 0.0

                                        Row(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        top = 12.dp
                                                    ),

                                            horizontalArrangement =
                                                Arrangement.spacedBy(10.dp)
                                        ) {

                                            Button(
                                                onClick = {

                                                    if (
                                    optionLots > 0 &&
                                        (optionPrice > 0.0 || !MarketClock.isOpen())
                                ) {

                                    optionTradeMessage =
                                        fnoState.placeOrder(
                                            contract = contract,
                                            side = TradeSide.BUY,
                                            lots = optionLots,
                                            price = optionPrice
                                        ).message
                                }
                                                },

                                                modifier =
                                                    Modifier.weight(1f),

                                                enabled =
                                                    optionLots > 0 &&
                                                        (optionPrice > 0.0 || !MarketClock.isOpen())
                                            ) {
                                                Text("BUY")
                                            }

                                            OutlinedButton(
                                                onClick = {

                                                    if (
                                    optionLots > 0 &&
                                        (optionPrice > 0.0 || !MarketClock.isOpen())
                                ) {

                                    optionTradeMessage =
                                        fnoState.placeOrder(
                                            contract = contract,
                                            side = TradeSide.SELL,
                                            lots = optionLots,
                                            price = optionPrice
                                        ).message
                                }
                                                },

                                                modifier =
                                                    Modifier.weight(1f),

                                                enabled =
                                                    optionLots > 0 &&
                                                        (optionPrice > 0.0 || !MarketClock.isOpen())
                                            ) {
                                                Text("SELL")
                                            }
                                        }

                                        if (
                                            optionTradeMessage
                                                .isNotBlank()
                                        ) {

                                            Text(
                                                text =
                                                    optionTradeMessage,

                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodyMedium,

                                                modifier =
                                                    Modifier.padding(
                                                        top = 10.dp
                                                    )
                                            )
                                        }
                                    }
                                }
                            }

                    Text(
                        text =
                            "Stored option contracts: $optionCount",

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

                        modifier =
                            Modifier.padding(
                                top = 12.dp
                            )
                    )
                }
            }
        }
    }
}
}
