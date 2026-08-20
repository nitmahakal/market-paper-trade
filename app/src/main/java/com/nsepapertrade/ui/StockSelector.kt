package com.nsepapertrade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nsepapertrade.model.Instrument

@Composable
fun StockSelector(
    instruments: List<Instrument>,
    selectedInstrument: Instrument?,
    onInstrumentSelected: (Instrument) -> Unit
) {
    var query by remember {
        mutableStateOf("")
    }

    val filtered = remember(query, instruments) {
        val text = query.trim()

        if (text.isEmpty()) {
            instruments
        } else {
            instruments.filter {
                it.symbol.contains(text, ignoreCase = true) ||
                it.name.contains(text, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Search NSE Stock")
            },
            singleLine = true
        )

        if (selectedInstrument != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Column {
                        Text(selectedInstrument.symbol)
                        Text(selectedInstrument.name)
                        Text(selectedInstrument.exchange)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            filtered.forEach { instrument ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable {
                            onInstrumentSelected(instrument)
                            query = instrument.symbol
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(instrument.symbol)
                        Text(instrument.name)
                    }
                }
            }
        }
    }
}
