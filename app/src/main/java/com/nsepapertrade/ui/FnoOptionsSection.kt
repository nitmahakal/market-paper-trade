package com.nsepapertrade.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.FnoOptionChain
import com.nsepapertrade.data.FnoOptionChainContract
import com.nsepapertrade.data.FnoOptionChainProvider
import com.nsepapertrade.data.FnoTradeState
import com.nsepapertrade.data.MarketClock
import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.OptionType
import com.nsepapertrade.model.TradeSide
import com.nsepapertrade.ui.theme.lossColor
import com.nsepapertrade.ui.theme.onTradeColor
import com.nsepapertrade.ui.theme.profitColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

private const val MAX_UNDERLYINGS = 10

private val ItmColor = Color(0xFFFFB300).copy(alpha = 0.16f)
private val OtmColor = Color(0xFF90A4AE).copy(alpha = 0.10f)

@Composable
fun FnoOptionsSection(
    optionContracts: List<FnoContract>,
    fnoState: FnoTradeState,
    provider: FnoOptionChainProvider
) {
    var search by remember { mutableStateOf("") }
    var selectedUnderlying by remember { mutableStateOf("") }
    var selectedExpiry by remember { mutableStateOf("") }
    var chain by remember { mutableStateOf<FnoOptionChain?>(null) }
    var selectedOption by remember {
        mutableStateOf<FnoContract?>(null)
    }
    var lotsText by remember { mutableStateOf("1") }
    var tradeMessage by remember { mutableStateOf("") }
    var chainMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var scrolledKey by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // ---- Data derived from the stored contract list ----

    val underlyings = remember(optionContracts) {
        optionContracts
            .map { it.underlying }
            .distinct()
            .sorted()
    }

    val lotByUnderlyingExpiry = remember(optionContracts) {
        val map = HashMap<String, Int>()

        optionContracts.forEach {
            if (it.lotSize > 0) {
                map[it.underlying.uppercase() + "|" + it.expiry] =
                    it.lotSize
            }
        }

        map
    }

    val lotByUnderlying = remember(optionContracts) {
        val map = HashMap<String, Int>()

        optionContracts.forEach {
            if (it.lotSize > 0) {
                map[it.underlying.uppercase()] = it.lotSize
            }
        }

        map
    }

    fun lotFor(underlying: String, expiry: String): Int {
        val key = underlying.uppercase()

        return lotByUnderlyingExpiry["$key|$expiry"]
            ?: lotByUnderlying[key]
            ?: 1
    }

    val expiries = remember(optionContracts, selectedUnderlying) {
        if (selectedUnderlying.isEmpty()) {
            emptyList()
        } else {
            val forUnderlying = optionContracts.filter {
                it.underlying.equals(
                    selectedUnderlying,
                    ignoreCase = true
                )
            }

            val active = forUnderlying.filter { it.isActive }

            (if (active.isNotEmpty()) active else forUnderlying)
                .map { it.expiry }
                .distinct()
                .sortedBy { expiryMillis(it) }
        }
    }

    val matches = remember(underlyings, search, selectedUnderlying) {
        if (selectedUnderlying.isNotEmpty()) {
            emptyList()
        } else {
            matchUnderlyings(underlyings, search)
        }
    }

    // The expiry whose chain we load: the chosen one, else the
    // nearest (just to read the asset price).
    val expiryToFetch =
        if (selectedExpiry.isNotEmpty()) {
            selectedExpiry
        } else {
            expiries.firstOrNull() ?: ""
        }

    // ---- Chain + asset price, refreshed while an asset is open ----

    LaunchedEffect(selectedUnderlying, expiryToFetch) {

        chain = null
        chainMessage = ""

        if (selectedUnderlying.isEmpty() || expiryToFetch.isEmpty()) {
            return@LaunchedEffect
        }

        while (isActive) {

            loading = true

            try {
                val result = withContext(Dispatchers.IO) {
                    provider.getOptionChain(
                        underlying = selectedUnderlying,
                        expiry = expiryToFetch
                    )
                }

                chain = result

                chainMessage =
                    if (result == null) {
                        "No option chain data available."
                    } else {
                        ""
                    }

                // Keep the open position's price in step.
                val option = selectedOption

                if (result != null && option != null) {
                    val ltp = ltpOf(result, option)

                    if (ltp > 0.0) {
                        fnoState.updateMarketPrice(
                            contractSymbol = option.symbol,
                            price = ltp
                        )
                    }
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                chainMessage =
                    e.message ?: "Option chain error."
            } finally {
                loading = false
            }

            delay(
                if (MarketClock.isOpen()) 5_000L else 60_000L
            )
        }
    }

    // Start the list at the ATM strike (once per expiry).
    val currentChain = chain
    val spotInfo = currentChain?.let { spotOf(it) }
    val spot = spotInfo?.first ?: 0.0
    val atmStrike =
        if (currentChain != null && spot > 0.0) {
            currentChain.rows
                .minByOrNull { abs(it.strikePrice - spot) }
                ?.strikePrice
        } else {
            null
        }

    val scrollKey = "$selectedUnderlying|$selectedExpiry"

    LaunchedEffect(currentChain != null, scrollKey, atmStrike) {
        val c = currentChain

        if (c != null &&
            selectedExpiry.isNotEmpty() &&
            atmStrike != null &&
            scrolledKey != scrollKey
        ) {
            val index = c.rows.indexOfFirst {
                it.strikePrice == atmStrike
            }

            if (index >= 0) {
                listState.scrollToItem((index - 3).coerceAtLeast(0))
                scrolledKey = scrollKey
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        // ---------- Step 1: choose the asset ----------

        if (selectedUnderlying.isEmpty()) {

            Text(
                text = "Option Chain",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp)
            )

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text("Search underlying") },
                placeholder = {
                    Text("NIFTY / BANKNIFTY / RELIANCE")
                },
                singleLine = true
            )

            if (search.isNotBlank()) {
                if (matches.isEmpty()) {
                    Text(
                        text = "No match",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = 16.dp,
                            top = 8.dp
                        )
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme
                                .colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Column {
                            matches.forEach { name ->
                                key(name) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme
                                            .typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme
                                            .colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedUnderlying = name
                                                selectedExpiry = ""
                                                selectedOption = null
                                                tradeMessage = ""
                                                scrolledKey = ""
                                            }
                                            .padding(
                                                horizontal = 16.dp,
                                                vertical = 12.dp
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

        } else {

            // ---------- Asset header with its price ----------

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme
                        .colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column {
                        Text(
                            text = selectedUnderlying,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (spot > 0.0) {
                                (if (spotInfo?.second == true) "≈ " else "") +
                                    "₹" + money(spot)
                            } else if (loading) {
                                "Fetching price…"
                            } else {
                                "--"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Text(
                        text = "✕ Change",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                selectedUnderlying = ""
                                selectedExpiry = ""
                                selectedOption = null
                                search = ""
                                tradeMessage = ""
                            }
                            .padding(8.dp)
                    )
                }
            }

            // ---------- Step 2: choose the expiry ----------

            if (selectedExpiry.isEmpty()) {

                Text(
                    text = "Select Expiry",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp)
                )

                if (expiries.isEmpty()) {
                    Text(
                        text = "No expiry found for this asset.",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme
                                .colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                        ) {
                            items(
                                items = expiries,
                                key = { it }
                            ) { expiry ->
                                Text(
                                    text = expiry,
                                    style = MaterialTheme
                                        .typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedExpiry = expiry
                                            selectedOption = null
                                            tradeMessage = ""
                                        }
                                        .padding(
                                            horizontal = 16.dp,
                                            vertical = 12.dp
                                        )
                                )
                            }
                        }
                    }
                }

            } else {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expiry: $selectedExpiry",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "✕ Change",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                selectedExpiry = ""
                                selectedOption = null
                                tradeMessage = ""
                            }
                            .padding(8.dp)
                    )
                }

                // ---------- Step 3: the chain, or the chosen option ----------

                val option = selectedOption

                if (option == null) {

                    ChainLegend()

                    if (currentChain == null) {
                        Text(
                            text = if (chainMessage.isNotBlank()) {
                                chainMessage
                            } else {
                                "Loading option chain…"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (chainMessage.isNotBlank()) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(
                                start = 4.dp,
                                top = 8.dp
                            )
                        )
                    } else {

                        if (chainMessage.isNotBlank()) {
                            Text(
                                text = chainMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(
                                    start = 4.dp,
                                    top = 8.dp
                                )
                            )
                        }

                        ChainHeader()

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                        ) {
                            items(
                                items = currentChain.rows,
                                key = { it.strikePrice }
                            ) { row ->

                                ChainRowView(
                                    strike = row.strikePrice,
                                    call = row.call,
                                    put = row.put,
                                    spot = spot,
                                    isAtm = row.strikePrice == atmStrike,
                                    onSelect = { picked ->
                                        selectedOption =
                                            picked.contract.copy(
                                                lotSize = lotFor(
                                                    picked.contract
                                                        .underlying,
                                                    picked.contract.expiry
                                                )
                                            )
                                        lotsText = "1"
                                        tradeMessage = ""
                                    }
                                )
                            }
                        }
                    }

                } else {

                    val ltp =
                        currentChain?.let { ltpOf(it, option) } ?: 0.0

                    SelectedOption(
                        contract = option,
                        ltp = ltp,
                        lotsText = lotsText,
                        onLotsChange = { text ->
                            lotsText = text.filter { it.isDigit() }
                        },
                        tradeMessage = tradeMessage,
                        onChange = {
                            selectedOption = null
                            tradeMessage = ""
                        },
                        onTrade = { side, lots, price ->
                            tradeMessage = fnoState.placeOrder(
                                contract = option,
                                side = side,
                                lots = lots,
                                price = price
                            ).message
                        }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------
// Chain table
// ---------------------------------------------------------

@Composable
private fun ChainLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendChip(
            "ATM",
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        )
        LegendChip("ITM", ItmColor)
        LegendChip("OTM", OtmColor)

        Text(
            text = "Tap a CE / PE price to trade",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendChip(
    label: String,
    color: Color
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .background(color)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun ChainHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderText("CE LTP", Modifier.weight(1f))
        HeaderText("STRIKE", Modifier.weight(0.9f))
        HeaderText("PE LTP", Modifier.weight(1f))
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ChainRowView(
    strike: Double,
    call: FnoOptionChainContract?,
    put: FnoOptionChainContract?,
    spot: Double,
    isAtm: Boolean,
    onSelect: (FnoOptionChainContract) -> Unit
) {
    val atmColor =
        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)

    // A call is in the money below the price, a put above it.
    val callColor =
        when {
            isAtm -> atmColor
            spot > 0.0 && strike < spot -> ItmColor
            else -> OtmColor
        }

    val putColor =
        when {
            isAtm -> atmColor
            spot > 0.0 && strike > spot -> ItmColor
            else -> OtmColor
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        PriceCell(
            option = call,
            color = callColor,
            modifier = Modifier.weight(1f),
            onSelect = onSelect
        )

        Text(
            text = strikeText(strike),
            modifier = Modifier
                .weight(0.9f)
                .padding(vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isAtm) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        PriceCell(
            option = put,
            color = putColor,
            modifier = Modifier.weight(1f),
            onSelect = onSelect
        )
    }
}

@Composable
private fun PriceCell(
    option: FnoOptionChainContract?,
    color: Color,
    modifier: Modifier,
    onSelect: (FnoOptionChainContract) -> Unit
) {
    Box(
        modifier = modifier
            .background(color)
            .then(
                if (option != null) {
                    Modifier.clickable { onSelect(option) }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (option != null && option.ltp > 0.0) {
                money(option.ltp)
            } else {
                "--"
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ---------------------------------------------------------
// The chosen option
// ---------------------------------------------------------

@Composable
private fun SelectedOption(
    contract: FnoContract,
    ltp: Double,
    lotsText: String,
    onLotsChange: (String) -> Unit,
    tradeMessage: String,
    onChange: () -> Unit,
    onTrade: (TradeSide, Int, Double) -> Unit
) {
    val lots = lotsText.toIntOrNull() ?: 0

    val canOrder =
        lots > 0 && (ltp > 0.0 || !MarketClock.isOpen())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
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
                Text(
                    text = contract.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "✕ Change strike",
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
                InfoCell(
                    "LOT SIZE",
                    contract.lotSize.toString(),
                    Modifier.weight(1f)
                )

                InfoCell(
                    "LTP",
                    if (ltp > 0.0) money(ltp) else "--",
                    Modifier.weight(1f)
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

            if (lots > 0 && ltp > 0.0) {
                Text(
                    text = "Premium value ₹" +
                        "%,.2f".format(
                            Locale.US,
                            ltp * contract.lotSize * lots
                        ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = { onTrade(TradeSide.BUY, lots, ltp) },
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
                    onClick = { onTrade(TradeSide.SELL, lots, ltp) },
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

// ---------------------------------------------------------
// Helpers
// ---------------------------------------------------------

private fun matchUnderlyings(
    all: List<String>,
    rawQuery: String
): List<String> {

    val query = rawQuery.trim().lowercase(Locale.ROOT)

    if (query.isEmpty()) {
        return emptyList()
    }

    val starts = ArrayList<String>()
    val contains = ArrayList<String>()

    for (name in all) {
        val key = name.lowercase(Locale.ROOT)

        if (key.startsWith(query)) {
            starts.add(name)
        } else if (key.contains(query)) {
            contains.add(name)
        }

        if (starts.size >= MAX_UNDERLYINGS) {
            break
        }
    }

    return (starts + contains).take(MAX_UNDERLYINGS)
}

// LTP of the chosen option, read from the chain.
private fun ltpOf(
    chain: FnoOptionChain,
    option: FnoContract
): Double {

    val row = chain.rows.firstOrNull {
        abs(it.strikePrice - option.strikePrice) < 0.001
    } ?: return 0.0

    val side =
        if (option.optionType == OptionType.CE) {
            row.call
        } else {
            row.put
        }

    return side?.ltp ?: 0.0
}

// Price of the asset: what NSE sent, else an estimate from the
// strike where CE and PE cost about the same. Second value is
// true when it is only an estimate.
private fun spotOf(chain: FnoOptionChain): Pair<Double, Boolean> {

    if (chain.underlyingValue > 0.0) {
        return chain.underlyingValue to false
    }

    var bestGap = Double.MAX_VALUE
    var estimate = 0.0

    for (row in chain.rows) {
        val ce = row.call?.ltp ?: 0.0
        val pe = row.put?.ltp ?: 0.0

        if (ce > 0.0 && pe > 0.0) {
            val gap = abs(ce - pe)

            if (gap < bestGap) {
                bestGap = gap
                estimate = row.strikePrice + ce - pe
            }
        }
    }

    return estimate to true
}

private fun strikeText(strike: Double): String {
    return if (strike % 1.0 == 0.0) {
        "%,.0f".format(Locale.US, strike)
    } else {
        "%,.2f".format(Locale.US, strike)
    }
}

private fun money(value: Double): String {
    return "%,.2f".format(Locale.US, value)
}
