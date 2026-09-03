package com.nsepapertrade.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nsepapertrade.data.PortfolioSnapshot
import com.nsepapertrade.model.Instrument
import com.nsepapertrade.data.MarketQuote

@Composable
fun PaperTradeScreen(
    snapshot: PortfolioSnapshot,
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

    val validQuantity = quantity.toIntOrNull()?.takeIf { it > 0 }
    val validPrice = price.toDoubleOrNull()?.takeIf { it > 0.0 }

    val canPlaceOrder =
        selectedInstrument != null &&
        validQuantity != null &&
        validPrice != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .navigationBarsPadding()
            .padding(16.dp)
    ) {

        Text(
            text = "NSE Paper Trade",
            style = MaterialTheme.typography.headlineSmall
        )

        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(16.dp)
        )

        PortfolioSummary(snapshot = snapshot)

        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(16.dp)
        )

        StockSelector(
            instruments = instruments,
            selectedInstrument = selectedInstrument,
            onInstrumentSelected = {
                onInstrumentSelected(it)

                if (it != null) {
                    price = ""
                    quantity = ""
                } else {
                    price = ""
                    quantity = ""
                }
            }
        )

        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Paper Order",
                    style = MaterialTheme.typography.titleMedium
                )

                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("LTP") },
                    singleLine = true
                )

                androidx.compose.foundation.layout.Spacer(
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

        androidx.compose.foundation.layout.Spacer(
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

        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (message.isNotBlank()) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}
