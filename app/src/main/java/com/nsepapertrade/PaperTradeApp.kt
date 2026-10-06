package com.nsepapertrade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.FnoContractStore
import com.nsepapertrade.data.FnoOptionChainState
import com.nsepapertrade.data.InstrumentRepository
import com.nsepapertrade.data.MarketDataState
import com.nsepapertrade.data.NseFnoMarketDataProvider
import com.nsepapertrade.data.PaperTradeEngine
import com.nsepapertrade.data.PaperTradeState
import com.nsepapertrade.data.YahooMarketDataProvider
import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.ui.PaperTradeScreen
import androidx.compose.foundation.layout.heightIn

@Composable
private fun FnoHomePlaceholder(
    selectedFnoSection: String,
    futureContracts: List<FnoContract>,
    optionCount: Int,
    onFuturesSelected: () -> Unit,
    onOptionsSelected: () -> Unit
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

    val filteredFutures = remember(
        futureContracts,
        searchQuery
    ) {
        val query = searchQuery.trim()

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

    val fnoMarketDataScope = rememberCoroutineScope()

    val fnoMarketDataState = remember {
        FnoMarketDataState(
            provider = NseFnoMarketDataProvider(),
            scope = fnoMarketDataScope
        )
    }

    val context = LocalContext.current

    val fnoTradeEngine = remember(context) {
        com.nsepapertrade.data.FnoTradeEngine(context)
    }

    LaunchedEffect(selectedFuture) {

        val contract = selectedFuture

        if (contract != null) {
            fnoMarketDataState.start(contract)
        } else {
            fnoMarketDataState.stop()
        }
    }

    val marketQuote = fnoMarketDataState.quote

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {

        Text(
            text = "Trade F&O",
            style = MaterialTheme.typography.headlineSmall
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (selectedFnoSection == "FUTURES") {

                Button(
                    onClick = onFuturesSelected,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Futures")
                }

            } else {

                OutlinedButton(
                    onClick = onFuturesSelected,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Futures")
                }
            }

            if (selectedFnoSection == "OPTIONS") {

                Button(
                    onClick = onOptionsSelected,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Options")
                }

            } else {

                OutlinedButton(
                    onClick = onOptionsSelected,
                    modifier = Modifier.weight(1f)
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
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                label = {
                    Text("Search Futures")
                },
                placeholder = {
                    Text("RELIANCE, NIFTY, BANKNIFTY...")
                },
                singleLine = true
            )

            if (searchQuery.isNotBlank()) {

                Text(
                    text = "${filteredFutures.size} futures contracts",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
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
                                selectedFuture = contract
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {

                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    text = contract.displayName
                                )

                                Text(
                                    text =
                                        "Expiry: ${contract.expiry}  •  Lot: ${contract.lotSize}",
                                    style =
                                        MaterialTheme.typography.bodySmall
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
                        text = "Selected Contract",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = contract.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = "Underlying: ${contract.underlying}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Expiry: ${contract.expiry}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Lot Size: ${contract.lotSize}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (fnoMarketDataState.isLoading) {

                        Text(
                            text = "Loading LTP...",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    marketQuote?.let { quote ->

                        Text(
                            text = "LTP: ${"%.2f".format(quote.price)}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (fnoMarketDataState.errorMessage.isNotBlank()) {

                        Text(
                            text = fnoMarketDataState.errorMessage,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = lotsText,
                        onValueChange = {
                            lotsText =
                                it.filter { character ->
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
                        lotsText.toIntOrNull() ?: 0

                    val price =
                        marketQuote?.price ?: 0.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Button(
                            onClick = {

                                if (price > 0.0 && lots > 0) {

                                    fnoTradeEngine.buy(
                                        contract = contract,
                                        lots = lots,
                                        price = price
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled =
                                price > 0.0 &&
                                    lots > 0
                        ) {
                            Text("BUY")
                        }

                        OutlinedButton(
                            onClick = {

                                if (price > 0.0 && lots > 0) {

                                    fnoTradeEngine.sell(
                                        contract = contract,
                                        lots = lots,
                                        price = price
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled =
                                price > 0.0 &&
                                    lots > 0
                        ) {
                            Text("SELL")
                        }
                    }
                }
            }

        } else {

            Text(
                text = "Options",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 20.dp)
            )

            Text(
                text = "Option chain will be connected next.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Stored option contracts: $optionCount",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
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
    onOptionsSelected: () -> Unit
) {

    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedFuture by remember {
        mutableStateOf<FnoContract?>(null)
    }

    val filteredFutures = remember(
        futureContracts,
        searchQuery
    ) {
        val query = searchQuery.trim()

        if (query.isBlank()) {
            futureContracts
        } else {
            futureContracts.filter {
                it.underlying.contains(
                    query,
                    ignoreCase = true
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {

        Text(
            text = "Trade F&O",
            style = MaterialTheme.typography.headlineSmall
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (selectedFnoSection == "FUTURES") {

                Button(
                    onClick = onFuturesSelected,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Futures")
                }

            } else {

                OutlinedButton(
                    onClick = onFuturesSelected,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Futures")
                }
            }

            if (selectedFnoSection == "OPTIONS") {

                Button(
                    onClick = onOptionsSelected,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Options")
                }

            } else {

                OutlinedButton(
                    onClick = onOptionsSelected,
                    modifier = Modifier.weight(1f)
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
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                label = {
                    Text("Search Futures")
                },
                placeholder = {
                    Text("RELIANCE, NIFTY, BANKNIFTY...")
                },
                singleLine = true
            )

            Text(
                text = "${filteredFutures.size} futures contracts",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                            selectedFuture = contract
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text = contract.displayName
                            )

                            Text(
                                text = "Expiry: ${contract.expiry}  •  Lot: ${contract.lotSize}",
                                style = MaterialTheme.typography.bodySmall
                            )
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
                        text = "Selected Contract",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = contract.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Text(
                        text = "Underlying: ${contract.underlying}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Expiry: ${contract.expiry}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Lot Size: ${contract.lotSize}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

        } else {

            Text(
                text = "Options",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 20.dp)
            )

            Text(
                text = "Option chain will be connected next.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Stored option contracts: $optionCount",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
