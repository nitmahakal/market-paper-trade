package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.FnoUnderlyingType
import com.nsepapertrade.model.OptionType

class NseFnoContractDataParser {

    fun parseCsv(lines: List<String>): List<FnoContract> {
        if (lines.isEmpty()) return emptyList()

        val header = parseLine(lines.first())

        val index = header.mapIndexed { position, name ->
            name.trim().uppercase() to position
        }.toMap()

        fun get(values: List<String>, vararg names: String): String? {
            for (name in names) {
                val position = index[name.uppercase()]
                if (position != null) {
                    return values.getOrNull(position)?.trim()
                }
            }
            return null
        }

        val contracts = mutableListOf<FnoContract>()

        for (line in lines.drop(1)) {
            if (line.isBlank()) continue

            val values = parseLine(line)

            val underlying =
                get(values, "SYMBOL", "UNDERLYING")
                    ?.takeIf { it.isNotBlank() }
                    ?: continue

            val expiry =
                get(values, "EXPIRY_DT", "EXPIRY")
                    ?.takeIf { it.isNotBlank() }
                    ?: continue

            val instrument =
                get(values, "INSTRUMENT", "INSTRUMENT_TYPE")
                    ?.uppercase()
                    ?: continue

            val lotSize =
                get(values, "LOT_SIZE", "MARKET_LOT")
                    ?.toIntOrNull()
                    ?: continue

            val strikePrice =
                get(values, "STRIKE_PR", "STRIKE_PRICE")
                    ?.toDoubleOrNull()
                    ?: 0.0

            val optionType = when (
                get(values, "OPTION_TYP", "OPTION_TYPE")
                    ?.uppercase()
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

                else -> continue
            }

            if (
                contractType == FnoContractType.OPTION &&
                optionType == OptionType.NONE
            ) {
                continue
            }

            val underlyingType =
                detectUnderlyingType(underlying)

            contracts.add(
                FnoContract(
                    underlying = underlying.uppercase(),
                    underlyingType = underlyingType,
                    contractType = contractType,
                    expiry = expiry,
                    strikePrice = strikePrice,
                    optionType = optionType,
                    lotSize = lotSize
                )
            )
        }

        return contracts.distinctBy {
            ContractKey.from(it)
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
        var quoted = false

        for (character in line) {
            when {
                character == '"' -> {
                    quoted = !quoted
                }

                character == ',' && !quoted -> {
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

    private object ContractKey {
        fun from(contract: FnoContract): String {
            return listOf(
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
}
