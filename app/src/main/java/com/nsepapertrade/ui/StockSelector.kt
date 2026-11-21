package com.nsepapertrade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nsepapertrade.model.Instrument
import java.util.Locale

private const val MAX_RESULTS = 8

private class SearchEntry(
    val instrument: Instrument,
    val symbolKey: String,
    val nameKey: String
)

@Composable
fun StockSelector(
    instruments: List<Instrument>,
    selectedInstrument: Instrument?,
    onInstrumentSelected: (Instrument?) -> Unit,
    onSelectedFocused: () -> Unit = {}
) {
    var query by remember {
        mutableStateOf("")
    }

    // Lowercase keys are built once, not on every keystroke.
    val index = remember(instruments) {
        instruments.map {
            SearchEntry(
                instrument = it,
                symbolKey = it.symbol.lowercase(Locale.ROOT),
                nameKey = it.name.lowercase(Locale.ROOT)
            )
        }
    }

    // Selection can also come from outside (portfolio tap):
    // show that symbol in the search box.
    LaunchedEffect(selectedInstrument?.symbol) {
        if (selectedInstrument != null) {
            query = selectedInstrument.symbol
        }
    }

    val results = remember(query, index, selectedInstrument) {
        if (selectedInstrument != null) {
            emptyList()
        } else {
            search(index, query)
        }
    }

    fun choose(instrument: Instrument) {
        query = instrument.symbol
        onInstrumentSelected(instrument)
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        OutlinedTextField(
            value = query,
            onValueChange = { newText ->
                query = newText

                val editedAwayFromSelection =
                    selectedInstrument != null &&
                        !newText.trim().equals(
                            selectedInstrument.symbol,
                            ignoreCase = true
                        )

                if (newText.isBlank() || editedAwayFromSelection) {
                    onInstrumentSelected(null)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focus ->
                    if (focus.isFocused &&
                        selectedInstrument != null
                    ) {
                        onSelectedFocused()
                    }
                },
            label = {
                Text("Search NSE Stock")
            },
            placeholder = {
                Text("Symbol or company name")
            },
            singleLine = true,
            trailingIcon = if (query.isNotEmpty()) {
                {
                    Text(
                        text = "✕",
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable {
                                query = ""
                                onInstrumentSelected(null)
                            }
                            .padding(12.dp)
                    )
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    results.firstOrNull()?.let { choose(it) }
                }
            )
        )

        if (results.isNotEmpty()) {

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
                    results.forEach { instrument ->

                        key(instrument.symbol) {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { choose(instrument) }
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 10.dp
                                    )
                            ) {
                                Text(
                                    text = instrument.symbol,
                                    style =
                                        MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                        MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = instrument.name,
                                    style =
                                        MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme
                                        .colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

        } else if (
            selectedInstrument == null &&
            query.isNotBlank()
        ) {
            Text(
                text = "No match",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 8.dp
                )
            )
        }
    }
}

// Best matches first: symbol starts with, then symbol
// contains, then company name contains. Stops at MAX_RESULTS.
private fun search(
    index: List<SearchEntry>,
    rawQuery: String
): List<Instrument> {

    val text = rawQuery.trim().lowercase(Locale.ROOT)

    if (text.isEmpty()) {
        return emptyList()
    }

    val found = ArrayList<Instrument>(MAX_RESULTS)

    for (entry in index) {
        if (entry.symbolKey.startsWith(text)) {
            found.add(entry.instrument)
            if (found.size >= MAX_RESULTS) return found
        }
    }

    for (entry in index) {
        if (!entry.symbolKey.startsWith(text) &&
            entry.symbolKey.contains(text)
        ) {
            found.add(entry.instrument)
            if (found.size >= MAX_RESULTS) return found
        }
    }

    for (entry in index) {
        if (!entry.symbolKey.contains(text) &&
            entry.nameKey.contains(text)
        ) {
            found.add(entry.instrument)
            if (found.size >= MAX_RESULTS) return found
        }
    }

    return found
}
