package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.FnoUnderlyingType
import com.nsepapertrade.model.OptionType

class NseFnoContractParser {

    fun parse(lines: List<String>): List<FnoContract> {
        if (lines.isEmpty()) return emptyList()

        val header = splitCsvLine(lines.first())

        val columns = header
            .mapIndexed { index, name ->
                name.trim().uppercase() to index
            }
            .toMap()

        val result = mutableListOf<FnoContract>()

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue

            try {
                val values = splitCsvLine(line)

                fun value(name: String): String? {
                    val index = columns[name] ?: return null
                    return values.getOrNull(index)?.trim()
                }

                val symbol =
                    value("SYMBOL")
                        ?: value("UNDERLYING")
                        ?: continue

                val expiry =
                    value("EXPIRY_DT")
                        ?: value("EXPIRY")
                        ?: continue

                val instrument =
                    value("INSTRUMENT")
                        ?: value("INSTRUMENT_TYPE")
                        ?: continue

                val strike =
                    value("STRIKE_PR")
                        ?: value("STRIKE_PRICE")
                        ?: "0"

                val optionType =
                    value("OPTION_TYP")
                        ?: value("OPTION_TYPE")
                        ?: "XX"

                val lotSize =
                    value("LOT_SIZE")
                        ?: continue

                val contractType = when {
                    instrument.uppercase().contains("FUT") ->
                        FnoContractType.FUTURE

                    instrument.uppercase().contains("OPT") ->
                        FnoContractType.OPTION

                    else -> continue
                }

                val parsedOptionType = when (optionType.uppercase()) {
                    "CE" -> OptionType.CE
                    "PE" -> OptionType.PE
                    else -> OptionType.NONE
                }

                val parsedStrike =
                    strike.toDoubleOrNull() ?: 0.0

                val parsedLotSize =
                    lotSize.toIntOrNull() ?: continue

                val underlyingType =
                    if (isIndex(symbol)) {
                        FnoUnderlyingType.INDEX
                    } else {
                        FnoUnderlyingType.STOCK
                    }

                result.add(
                    FnoContract(
                        underlying = symbol,
                        underlyingType = underlyingType,
                        contractType = contractType,
                        expiry = expiry,
                        strikePrice = parsedStrike,
                        optionType = parsedOptionType,
                        lotSize = parsedLotSize
                    )
                )
            } catch (_: Exception) {
                // Ignore malformed contract rows.
            }
        }

        return result.distinctBy {
            listOf(
                it.underlying.uppercase(),
                it.underlyingType,
                it.contractType,
                it.expiry,
                it.strikePrice,
                it.optionType,
                it.lotSize
            )
        }
    }

    private fun isIndex(symbol: String): Boolean {
        return symbol.uppercase() in setOf(
            "NIFTY",
            "BANKNIFTY",
            "FINNIFTY",
            "MIDCPNIFTY",
            "NIFTYNXT50"
        )
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false

        for (char in line) {
            when {
                char == '"' -> {
                    insideQuotes = !insideQuotes
                }

                char == ',' && !insideQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }

                else -> {
                    current.append(char)
                }
            }
        }

        result.add(current.toString())
        return result
    }
}
