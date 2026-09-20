package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.FnoUnderlyingType
import com.nsepapertrade.model.OptionType

class NseFnoContractParser {

    fun parse(lines: List<String>): List<FnoContract> {
        if (lines.isEmpty()) return emptyList()

        val header = parseLine(lines.first())

        val columns = header.mapIndexed { index, name ->
            name.trim().uppercase() to index
        }.toMap()

        fun getValue(
            values: List<String>,
            vararg names: String
        ): String? {
            for (name in names) {
                val index = columns[name.uppercase()]
                if (index != null) {
                    return values.getOrNull(index)?.trim()
                }
            }
            return null
        }

        val contracts = mutableListOf<FnoContract>()

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue

            try {
                val values = parseLine(line)

                val underlying =
                    getValue(
                        values,
                        "SYMBOL",
                        "UNDERLYING"
                    )?.takeIf { it.isNotBlank() }
                        ?: continue

                val expiry =
                    getValue(
                        values,
                        "EXPIRY_DT",
                        "EXPIRY"
                    )?.takeIf { it.isNotBlank() }
                        ?: continue

                val instrument =
                    getValue(
                        values,
                        "INSTRUMENT",
                        "INSTRUMENT_TYPE"
                    )?.uppercase()
                        ?: continue

                val lotSize =
                    getValue(
                        values,
                        "LOT_SIZE",
                        "MARKET_LOT"
                    )?.toIntOrNull()
                        ?: continue

                val strikePrice =
                    getValue(
                        values,
                        "STRIKE_PR",
                        "STRIKE_PRICE"
                    )?.toDoubleOrNull()
                        ?: 0.0

                val optionType = when (
                    getValue(
                        values,
                        "OPTION_TYP",
                        "OPTION_TYPE"
                    )?.uppercase()
                ) {
                    "CE" -> OptionType.CE
                    "PE" -> OptionType.PE
                    else -> OptionType.NONE
                }

                val contractType = when {
                    instrument.contains("FUT") ->
                        FnoContractType.FUTURE

                    instrument.contains("OPT") ->
                        FnoContractType.OPTION

                    else ->
                        continue
                }

                if (
                    contractType == FnoContractType.OPTION &&
                    optionType == OptionType.NONE
                ) {
                    continue
                }

                contracts.add(
                    FnoContract(
                        underlying = underlying.uppercase(),
                        underlyingType = detectUnderlyingType(
                            underlying
                        ),
                        contractType = contractType,
                        expiry = expiry,
                        strikePrice = strikePrice,
                        optionType = optionType,
                        lotSize = lotSize
                    )
                )
            } catch (_: Exception) {
                // Ignore malformed rows.
            }
        }

        return contracts.distinctBy { contract ->
            listOf(
                contract.underlying.uppercase(),
                contract.underlyingType.name,
                contract.contractType.name,
                contract.expiry,
                contract.strikePrice,
                contract.optionType.name,
                contract.lotSize
            ).joinToString("|")
        }
    }

    private fun detectUnderlyingType(
        underlying: String
    ): FnoUnderlyingType {
        return when (underlying.uppercase()) {
            "NIFTY",
            "BANKNIFTY",
            "FINNIFTY",
            "MIDCPNIFTY",
            "NIFTYNXT50" ->
                FnoUnderlyingType.INDEX

            else ->
                FnoUnderlyingType.STOCK
        }
    }

    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var insideQuotes = false

        for (character in line) {
            when {
                character == '"' -> {
                    insideQuotes = !insideQuotes
                }

                character == ',' && !insideQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }

                else -> {
                    current.append(character)
                }
            }
        }

        result.add(current.toString())

        return result
    }
}
