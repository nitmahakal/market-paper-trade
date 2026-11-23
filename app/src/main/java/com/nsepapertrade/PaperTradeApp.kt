package com.nsepapertrade

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.FnoContractManager
import com.nsepapertrade.data.FnoContractStore
import com.nsepapertrade.data.FnoMarginManager
import com.nsepapertrade.data.FnoTradeState
import com.nsepapertrade.data.InstrumentRepository
import com.nsepapertrade.data.MarketDataState
import com.nsepapertrade.data.NseFnoContractProvider
import com.nsepapertrade.data.NseFnoMarketDataProvider
import com.nsepapertrade.data.NseSpanMarginProvider
import com.nsepapertrade.data.OrderBook
import com.nsepapertrade.data.OrderKind
import com.nsepapertrade.data.PaperTradeEngine
import com.nsepapertrade.data.PaperTradeState
import com.nsepapertrade.data.YahooMarketDataProvider
import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.ui.FnoFuturesSection
import com.nsepapertrade.ui.FnoOptionsSection
import com.nsepapertrade.ui.FnoPositionsPanel
import com.nsepapertrade.ui.PaperTradeScreen

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

    val optionContracts =
        remember(fnoContracts) {
            fnoContracts.filter {
                it.contractType ==
                    FnoContractType.OPTION
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
                        optionContracts,

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

            FnoFuturesSection(
                futureContracts = futureContracts,
                fnoState = fnoState,
                provider = provider
            )

        } else {

            FnoOptionsSection(
                optionContracts = optionContracts,
                fnoState = fnoState,
                provider = provider
            )
        }

        Text(
            text = "Stored option contracts: $optionCount",
            style =
                MaterialTheme.typography.bodySmall,
            modifier =
                Modifier.padding(top = 16.dp, bottom = 24.dp)
        )
    }
}
