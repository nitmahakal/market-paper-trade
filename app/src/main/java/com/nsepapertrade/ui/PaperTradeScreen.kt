package com.nsepapertrade.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.MarketQuote
import com.nsepapertrade.data.PortfolioSnapshot
import com.nsepapertrade.model.Instrument
import com.nsepapertrade.model.Position
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
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    // [0] = scroll viewport, [1] = stock selector section
    val coordinates = remember {
        arrayOfNulls<LayoutCoordinates>(2)
    }

    var scrollRequest by remember { mutableStateOf(0) }

    LaunchedEffect(scrollRequest) {

        if (scrollRequest == 0) {
            return@LaunchedEffect
        }

        // wait one frame so the selected stock card is laid out
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

    LaunchedEffect(
        selectedInstrument?.symbol,
        marketQuote?.symbol,
        marketQuote?.price
    ) {
        if (
            selectedInstrument != null &&
            marketQuote?.symbol == selectedInstrument.symbol &&
            marketQuote.price > 0.0
        ) {
            price = marketQuote.price.toString()
        }
    }

    val validQuantity = quantity.toIntOrNull()?.takeIf { it > 0 }
    val validPrice = price.toDoubleOrNull()?.takeIf { it > 0.0 }

    val canPlaceOrder =
        selectedInstrument != null &&
        validQuantity != null &&
        validPrice != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates[0] = it }
            .verticalScroll(scrollState)
            .navigationBarsPadding()
            .padding(16.dp)
    ) {

        Text(
            text = "NSE Paper Trade",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

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
                    price = ""
                    quantity = ""
                    scrollRequest++
                }
            }
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates[1] = it }
        ) {
            StockSelector(
                instruments = instruments,
                selectedInstrument = selectedInstrument,
                onInstrumentSelected = {
                    onInstrumentSelected(it)
                    price = ""
                    quantity = ""
                }
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Paper Order",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("LTP") },
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Quantity") },
                    singleLine = true
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (canPlaceOrder) {

                Button(
                    onClick = {
                        onBuy(
                            selectedInstrument!!.symbol,
                            validQuantity!!,
                            validPrice!!
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text("BUY")
                }

                Button(
                    onClick = {
                        onSell(
                            selectedInstrument!!.symbol,
                            validQuantity!!,
                            validPrice!!
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) {
                    Text("SELL")
                }

            } else {

                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("BUY")
                }

                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("SELL")
                }
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (message.isNotBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}
