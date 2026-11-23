package com.nsepapertrade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.FnoMarketDataProvider
import com.nsepapertrade.data.FnoTradeState
import com.nsepapertrade.data.MarketClock
import com.nsepapertrade.data.MarketQuote
import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.TradeSide
import com.nsepapertrade.ui.theme.lossColor
import com.nsepapertrade.ui.theme.onTradeColor
import com.nsepapertrade.ui.theme.profitColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

private const val MAX_RESULTS = 30

// Expiry like 25-Nov-2026 as a number, for sorting by date.
internal fun expiryMillis(expiry: String): Long {
    return try {
        SimpleDateFormat("dd-MMM-yyyy", Locale.US)
            .parse(expiry.trim())
            ?.time
            ?: Long.MAX_VALUE
    } catch (e: Exception) {
        Long.MAX_VALUE
    }
}

@Composable
fun FnoFuturesSection(
    futureContracts: List<FnoContract>,
    fnoState: FnoTradeState,
    provider: FnoMarketDataProvider
) {
    var searchQuery by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<FnoContract?>(null) }
    var lotsText by remember { mutableStateOf("1") }
    var quote by remember { mutableStateOf<MarketQuote?>(null) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var tradeMessage by remember { mutableStateOf("") }

    val results = remember(futureContracts, searchQuery, selected) {
        if (selected != null) {
            emptyList()
        } else {
            searchFutures(futureContracts, searchQuery)
        }
    }

    // Price of the selected contract, refreshed while it is open.
    LaunchedEffect(selected) {

        quote = null
        errorMessage = ""
        tradeMessage = ""

        val contract = selected ?: return@LaunchedEffect

        while (isActive) {

            loading = true

            try {
                val result = withContext(Dispatchers.IO) {
                    provider.getQuote(contract)
                }

                quote = result

                if (result == null) {
                    errorMessage = "No market data available."
                } else {
                    errorMessage = ""

                    fnoState.updateMarketPrice(
                        contractSymbol = contract.symbol,
                        price = result.price
                    )
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage =
                    e.message ?: "F&O market data error."
            } finally {
                loading = false
            }

            delay(
                if (MarketClock.isOpen()) 5_000L else 60_000L
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        val contract = selected

        if (contract == null) {

            Text(
                text = "Select Futures Contract",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text("Search underlying") },
                placeholder = {
                    Text("RELIANCE / NIFTY / BANKNIFTY")
                },
                singleLine = true
            )

            if (searchQuery.isNotBlank()) {

                if (results.isEmpty()) {
                    Text(
                        text = "No contract found",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            top = 8.dp
                        )
                    )
                } else {
                    ResultsCard(
                        results = results,
                        onSelect = {
                            selected = it
                            lotsText = "1"
                        }
                    )

                    if (results.size >= MAX_RESULTS) {
                        Text(
                            text = "Showing first $MAX_RESULTS. " +
                                "Type more to narrow down.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme
                                .colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                top = 6.dp
                            )
                        )
                    }
                }
            }

        } else {

            SelectedFuture(
                contract = contract,
                quote = quote,
                loading = loading,
                errorMessage = errorMessage,
                lotsText = lotsText,
                onLotsChange = { text ->
                    lotsText = text.filter { it.isDigit() }
                },
                tradeMessage = tradeMessage,
                onChange = {
                    selected = null
                    searchQuery = ""
                },
                onTrade = { side, lots, price ->
                    tradeMessage = fnoState.placeOrder(
                        contract = contract,
                        side = side,
                        lots = lots,
                        price = price
                    ).message
                }
            )
        }
    }
}

@Composable
private fun ResultsCard(
    results: List<FnoContract>,
    onSelect: (FnoContract) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column {
            results.forEach { contract ->

                key(contract.symbol) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(contract) }
                            .padding(
                                horizontal = 16.dp,
                                vertical = 10.dp
                            )
                    ) {
                        Text(
                            text = contract.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Expiry ${contract.expiry}  •  " +
                                "Lot ${contract.lotSize}",
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedFuture(
    contract: FnoContract,
    quote: MarketQuote?,
    loading: Boolean,
    errorMessage: String,
    lotsText: String,
    onLotsChange: (String) -> Unit,
    tradeMessage: String,
    onChange: () -> Unit,
    onTrade: (TradeSide, Int, Double) -> Unit
) {
    val lots = lotsText.toIntOrNull() ?: 0
    val price = quote?.price ?: 0.0

    val canOrder =
        lots > 0 && (price > 0.0 || !MarketClock.isOpen())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contract.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Underlying ${contract.underlying}",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "✕ Change",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onChange)
                        .padding(8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoCell("EXPIRY", contract.expiry, Modifier.weight(1f))

                InfoCell(
                    "LOT SIZE",
                    contract.lotSize.toString(),
                    Modifier.weight(1f)
                )

                InfoCell(
                    "LTP",
                    if (price > 0.0) money(price) else "--",
                    Modifier.weight(1f)
                )
            }

            if (loading && quote == null) {
                Text(
                    text = "Fetching price…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (!MarketClock.isOpen()) {
                Text(
                    text = MarketClock.closedNotice(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = lotsText,
                onValueChange = onLotsChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Lots") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            if (lots > 0 && price > 0.0) {
                Text(
                    text = "Contract value ₹" +
                        "%,.2f".format(
                            Locale.US,
                            price * contract.lotSize * lots
                        ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = { onTrade(TradeSide.BUY, lots, price) },
                    enabled = canOrder,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = profitColor(),
                        contentColor = onTradeColor(),
                        disabledContainerColor =
                            MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme
                            .colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("BUY")
                }

                Button(
                    onClick = { onTrade(TradeSide.SELL, lots, price) },
                    enabled = canOrder,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = lossColor(),
                        contentColor = onTradeColor(),
                        disabledContainerColor =
                            MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme
                            .colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("SELL")
                }
            }

            if (tradeMessage.isNotBlank()) {
                Text(
                    text = tradeMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun InfoCell(
    label: String,
    value: String,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

// Names starting with the text first, then the rest;
// within a name, nearest expiry first.
private fun searchFutures(
    all: List<FnoContract>,
    rawQuery: String
): List<FnoContract> {

    val query = rawQuery.trim().lowercase(Locale.ROOT)

    if (query.isEmpty()) {
        return emptyList()
    }

    return all
        .filter {
            it.underlying.lowercase(Locale.ROOT).contains(query)
        }
        .sortedWith(
            compareBy<FnoContract>(
                {
                    !it.underlying.lowercase(Locale.ROOT)
                        .startsWith(query)
                },
                { it.underlying },
                { expiryMillis(it.expiry) }
            )
        )
        .take(MAX_RESULTS)
}

private fun money(value: Double): String {
    return "%,.2f".format(Locale.US, value)
}
