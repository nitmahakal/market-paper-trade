package com.nsepapertrade.data

import android.content.Context
import com.nsepapertrade.model.Instrument
import com.nsepapertrade.model.InstrumentType
import java.io.BufferedReader
import java.io.InputStreamReader

class InstrumentRepository(
    private val context: Context
) {

    private val equityInstruments: List<Instrument> by lazy {
        loadEquityInstruments()
    }

    fun getEquities(): List<Instrument> {
        return equityInstruments
    }

    fun searchEquities(query: String): List<Instrument> {
        val text = query.trim()

        if (text.isEmpty()) {
            return equityInstruments
        }

        return equityInstruments.filter {
            it.symbol.contains(text, ignoreCase = true) ||
                it.name.contains(text, ignoreCase = true)
        }
    }

    fun findBySymbol(symbol: String): Instrument? {
        return equityInstruments.firstOrNull {
            it.symbol.equals(symbol.trim(), ignoreCase = true)
        }
    }

    fun getByType(type: InstrumentType): List<Instrument> {
        return equityInstruments.filter {
            it.type == type
        }
    }

    private fun loadEquityInstruments(): List<Instrument> {
        val result = mutableListOf<Instrument>()

        context.assets.open("EQUITY_L.csv").use { input ->

            BufferedReader(
                InputStreamReader(input, Charsets.UTF_8)
            ).useLines { lines ->

                val iterator = lines.iterator()

                if (!iterator.hasNext()) {
                    return@useLines
                }

                val header = iterator.next()
                    .removePrefix("\uFEFF")

                val columns = parseCsvLine(header)
                    .map { it.trim() }

                val symbolIndex = columns.indexOfFirst {
                    it.equals("SYMBOL", ignoreCase = true)
                }

                val nameIndex = columns.indexOfFirst {
                    it.equals("NAME OF COMPANY", ignoreCase = true)
                }

                val seriesIndex = columns.indexOfFirst {
                    it.equals("SERIES", ignoreCase = true)
                }

                if (
                    symbolIndex < 0 ||
                    nameIndex < 0 ||
                    seriesIndex < 0
                ) {
                    return@useLines
                }

                while (iterator.hasNext()) {

                    val row = parseCsvLine(iterator.next())

                    if (
                        row.size <= symbolIndex ||
                        row.size <= nameIndex ||
                        row.size <= seriesIndex
                    ) {
                        continue
                    }

                    val symbol = row[symbolIndex].trim()
                    val name = row[nameIndex].trim()
                    val series = row[seriesIndex].trim()

                    if (
                        symbol.isBlank() ||
                        name.isBlank() ||
                        !series.equals("EQ", ignoreCase = true)
                    ) {
                        continue
                    }

                    result.add(
                        Instrument(
                            symbol = symbol.uppercase(),
                            name = name,
                            type = InstrumentType.EQUITY,
                            exchange = "NSE"
                        )
                    )
                }
            }
        }

        return result
            .distinctBy { it.symbol }
            .sortedBy { it.symbol }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()

        var insideQuotes = false
        var index = 0

        while (index < line.length) {

            val char = line[index]

            when {
                char == '"' -> {
                    if (
                        insideQuotes &&
                        index + 1 < line.length &&
                        line[index + 1] == '"'
                    ) {
                        current.append('"')
                        index++
                    } else {
                        insideQuotes = !insideQuotes
                    }
                }

                char == ',' && !insideQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }

                else -> {
                    current.append(char)
                }
            }

            index++
        }

        result.add(current.toString())

        return result
    }
}
