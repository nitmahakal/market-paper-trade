package com.nsepapertrade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.MarketQuote
import com.nsepapertrade.data.PortfolioSnapshot
import com.nsepapertrade.model.Instrument
import com.nsepapertrade.model.Position
import com.nsepapertrade.ui.theme.lossColor
import com.nsepapertrade.ui.theme.onTradeColor
import com.nsepapertrade.ui.theme.profitColor
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PaperTradeScreen(
    snapshot: PortfolioSnapshot,
    positions: List<Position>,
    marketQuote: MarketQuote?,
    message: String,
    instruments: List<Instrument>,
    selectedInstrument: Instrument?,
    onInstrumentSelected: (Instrument?) -> Unit,
    onBuy: (String, Int, Double) -> Unit,
    onSell: (String, Int, Double) -> Unit
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    // [0] = scroll viewport, [1] = stock selector section
    val coordinates = remember {
        arrayOfNulls<LayoutCoordinates>(2)
    }

    var showOrder by remember { mutableStateOf(false) }
    var awaitingTrade by remember { mutableStateOf(false) }
    var scrollRequest by remember { mutableStateOf(0) }

    // Scroll to the search box when a portfolio symbol is tapped
    LaunchedEffect(scrollRequest) {

        if (scrollRequest == 0) {
            return@LaunchedEffect
        }

        // wait one frame so the order panel is laid out
        withFrameNanos { }

        val viewport = coordinates[0]
        val selector = coordinates[1]

        if (
            viewport != null &&
            selector != null &&
            viewport.isAttached &&
            selector.isAttached
        ) {
            val delta =
                selector.positionInRoot().y -
                    viewport.positionInRoot().y

            val target =
                (scrollState.value + delta)
                    .roundToInt()
                    .coerceAtLeast(0)

            scrollState.animateScrollTo(target)
        }
    }

    // After a successful trade: close the order panel,
    // leave only the stock name in the search box.
    LaunchedEffect(snapshot, message) {
        if (awaitingTrade) {
            awaitingTrade = false

            if (
                message.startsWith("BUY successful") ||
                message.startsWith("SELL successful")
            ) {
                showOrder = false
                focusManager.clearFocus()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates[0] = it }
            .verticalScroll(scrollState)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {

        Text(
            text = "NSE Paper Trade",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        PortfolioSummary(
            snapshot = snapshot,
            positions = positions,
            onPositionClick = { position ->

                val instrument = instruments.firstOrNull {
                    it.symbol.equals(
                        position.symbol,
                        ignoreCase = true
                    )
                }

                if (instrument != null) {
                    onInstrumentSelected(instrument)
                    showOrder = true
                    scrollRequest++
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates[1] = it }
        ) {
            StockSelector(
                instruments = instruments,
                selectedInstrument = selectedInstrument,
                onInstrumentSelected = { instrument ->

                    onInstrumentSelected(instrument)

                    if (instrument != null) {
                        showOrder = true
                        focusManager.clearFocus()
                    } else {
                        showOrder = false
                    }
                },
                onSelectedFocused = {
                    showOrder = true
                }
            )
        }

        if (showOrder && selectedInstrument != null) {

            Spacer(modifier = Modifier.height(12.dp))

            key(selectedInstrument.symbol) {
                OrderPanel(
                    instrument = selectedInstrument,
                    marketQuote = marketQuote,
                    position = positions.firstOrNull {
                        it.quantity > 0 &&
                            it.symbol.equals(
                                selectedInstrument.symbol,
                                ignoreCase = true
                            )
                    },
                    onBuy = { symbol, quantity, price ->
                        awaitingTrade = true
                        onBuy(symbol, quantity, price)
                    },
                    onSell = { symbol, quantity, price ->
                        awaitingTrade = true
                        onSell(symbol, quantity, price)
                    }
                )
            }
        }

        if (message.isNotBlank()) {

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OrderPanel(
    instrument: Instrument,
    marketQuote: MarketQuote?,
    position: Position?,
    onBuy: (String, Int, Double) -> Unit,
    onSell: (String, Int, Double) -> Unit
) {
    // Own state: typing here recomposes only this panel.
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var priceEdited by remember { mutableStateOf(false) }

    val liveQuote = marketQuote?.takeIf {
        it.symbol == instrument.symbol && it.price > 0.0
    }

    // Keep price in step with the live quote until the user edits it.
    LaunchedEffect(liveQuote?.price) {
        if (!priceEdited && liveQuote != null) {
            price = formatPrice(liveQuote.price)
        }
    }

    val validQuantity = quantity.toIntOrNull()?.takeIf { it > 0 }
    val validPrice = price.toDoubleOrNull()?.takeIf { it > 0.0 }

    val canOrder = validQuantity != null && validPrice != null

    Card(
        modifier = Modifier.fillMaxWidth(),
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

            Column {
                Text(
                    text = instrument.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = instrument.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (position != null) {
                    "Holding: ${position.quantity} @ ₹" +
                        formatPrice(position.averagePrice)
                } else {
                    "No holding"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedTextField(
                    value = price,
                    onValueChange = { text ->
                        price = text.filter {
                            it.isDigit() || it == '.'
                        }
                        priceEdited = true
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Price (LTP)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { text ->
                        quantity = text.filter { it.isDigit() }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = if (liveQuote != null) {
                        "Quote: " + liveQuote.source.name
                            .lowercase(Locale.ROOT)
                            .replaceFirstChar { it.uppercase() }
                    } else {
                        "Fetching price…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (priceEdited && liveQuote != null) {
                    Text(
                        text = "Use live price",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            price = formatPrice(liveQuote.price)
                            priceEdited = false
                        }
                    )
                }
            }

            if (validQuantity != null && validPrice != null) {
                Text(
                    text = "Order value ₹" +
                        "%,.2f".format(
                            Locale.US,
                            validQuantity * validPrice
                        ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = {
                        val q = validQuantity
                        val p = validPrice

                        if (q != null && p != null) {
                            onBuy(instrument.symbol, q, p)
                        }
                    },
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
                    onClick = {
                        val q = validQuantity
                        val p = validPrice

                        if (q != null && p != null) {
                            onSell(instrument.symbol, q, p)
                        }
                    },
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
        }
    }
}

private fun formatPrice(value: Double): String {
    return "%.2f".format(Locale.US, value)
}
