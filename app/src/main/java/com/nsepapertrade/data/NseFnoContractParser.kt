package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.FnoUnderlyingType
import com.nsepapertrade.model.OptionType
import java.text.SimpleDateFormat
import java.util.Locale

class NseFnoContractParser {

    fun parse(lines: List<String>): List<FnoContract> {

        if (lines.isEmpty()) {
            return emptyList()
        }

        val headerIndex =
            lines.indexOfFirst {
                it.contains("FinInstrmNm", ignoreCase = true) &&
                    it.contains("TckrSymb", ignoreCase = true)
            }

        if (headerIndex < 0) {
            return emptyList()
        }

        val headers =
            splitCsvLine(lines[headerIndex])
                .map {
                    it.trim()
                        .removePrefix("\uFEFF")
                }

        val indexMap =
            headers.mapIndexed { index, value ->
                value.uppercase(Locale.US) to index
            }.toMap()

        val result = mutableListOf<FnoContract>()

        for (lineIndex in headerIndex + 1 until lines.size) {

            val line = lines[lineIndex]

            if (line.isBlank()) {
                continue
            }

            val values = splitCsvLine(line)

            val instrumentName =
                getValue(
                    values,
                    indexMap,
                    "FinInstrmNm"
                )?.uppercase(Locale.US)
                    ?: continue

            val underlying =
                getValue(
                    values,
                    indexMap,
                    "TckrSymb"
                )?.trim()
                    ?.uppercase(Locale.US)
                    ?: continue

            if (underlying.isBlank()) {
                continue
            }

            val lotSize =
                getValue(
                    values,
                    indexMap,
                    "MinLot",
                    "NewBrdLotQty"
                )
                    ?.toDoubleOrNull()
                    ?.toInt()
                    ?: continue

            if (lotSize <= 0) {
                continue
            }

            val expiry =
                resolveExpiry(
                    values,
                    indexMap
                ) ?: continue

            when {

                instrumentName == "FUTIDX" -> {
                    result +=
                        FnoContract(
                            underlying = underlying,
                            underlyingType = FnoUnderlyingType.INDEX,
                            contractType = FnoContractType.FUTURE,
                            expiry = expiry,
                            lotSize = lotSize
                        )
                }

                instrumentName == "FUTSTK" -> {
                    result +=
                        FnoContract(
                            underlying = underlying,
                            underlyingType = FnoUnderlyingType.STOCK,
                            contractType = FnoContractType.FUTURE,
                            expiry = expiry,
                            lotSize = lotSize
                        )
                }

                instrumentName == "OPTIDX" -> {

                    val strike =
                        parseStrike(
                            values,
                            indexMap
                        ) ?: continue

                    val optionType =
                        parseOptionType(
                            values,
                            indexMap
                        ) ?: continue

                    result +=
                        FnoContract(
                            underlying = underlying,
                            underlyingType = FnoUnderlyingType.INDEX,
                            contractType = FnoContractType.OPTION,
                            expiry = expiry,
                            strikePrice = strike,
                            optionType = optionType,
                            lotSize = lotSize
                        )
                }

                instrumentName == "OPTSTK" -> {

                    val strike =
                        parseStrike(
                            values,
                            indexMap
                        ) ?: continue

                    val optionType =
                        parseOptionType(
                            values,
                            indexMap
                        ) ?: continue

                    result +=
                        FnoContract(
                            underlying = underlying,
                            underlyingType = FnoUnderlyingType.STOCK,
                            contractType = FnoContractType.OPTION,
                            expiry = expiry,
                            strikePrice = strike,
                            optionType = optionType,
                            lotSize = lotSize
                        )
                }
            }
        }

        return result.distinctBy {
            buildString {
                append(it.underlying)
                append("|")
                append(it.underlyingType)
                append("|")
                append(it.contractType)
                append("|")
                append(it.expiry)
                append("|")
                append(it.strikePrice)
                append("|")
                append(it.optionType)
                append("|")
                append(it.lotSize)
            }
        }
    }

    private fun resolveExpiry(
        values: List<String>,
        indexMap: Map<String, Int>
    ): String? {

        val stockName =
            getValue(
                values,
                indexMap,
                "StockNm"
            )

        if (!stockName.isNullOrBlank()) {

            val suffix =
                stockName.trim()
                    .takeLast(8)

            val parsed =
                parseDate(
                    suffix,
                    "ddMMyyyy"
                )

            if (parsed != null) {
                return parsed
            }
        }

        val expiryValue =
            getValue(
                values,
                indexMap,
                "XpryDt",
                "EXPIRY_DT",
                "EXPIRY"
            )

        if (expiryValue.isNullOrBlank()) {
            return null
        }

        val cleaned =
            expiryValue.trim()

        val numericExpiry =
            cleaned.toLongOrNull()

        if (numericExpiry != null) {

            return try {

                val nseEpoch =
                    java.util.Calendar.getInstance(
                        java.util.TimeZone.getTimeZone("UTC")
                    ).apply {
                        clear()
                        set(
                            1980,
                            java.util.Calendar.JANUARY,
                            1,
                            0,
                            0,
                            0
                        )
                    }.timeInMillis

                val expiryMillis =
                    nseEpoch +
                        (numericExpiry * 1000L)

                SimpleDateFormat(
                    "dd-MMM-yyyy",
                    Locale.US
                ).format(
                    java.util.Date(expiryMillis)
                )

            } catch (_: Exception) {
                null
            }
        }

        parseDate(cleaned, "dd-MMM-yyyy")?.let {
            return it
        }

        parseDate(cleaned, "ddMMMyyyy")?.let {
            return it
        }

        parseDate(cleaned, "dd/MM/yyyy")?.let {
            return it
        }

        parseDate(cleaned, "dd-MM-yyyy")?.let {
            return it
        }

        parseDate(cleaned, "yyyy-MM-dd")?.let {
            return it
        }

        return cleaned
    }

    private fun parseStrike(
        values: List<String>,
        indexMap: Map<String, Int>
    ): Double? {

        val value =
            getValue(
                values,
                indexMap,
                "StrkPric",
                "STRIKE_PR",
                "STRIKE_PRICE"
            ) ?: return null

        return value
            .trim()
            .toDoubleOrNull()
            ?.takeIf { it >= 0.0 }
    }

    private fun parseOptionType(
        values: List<String>,
        indexMap: Map<String, Int>
    ): OptionType? {

        val value =
            getValue(
                values,
                indexMap,
                "OptnTp",
                "OPTION_TYP",
                "OPTION_TYPE"
            )
                ?.trim()
                ?.uppercase(Locale.US)
                ?: return null

        return when (value) {
            "CE" -> OptionType.CE
            "PE" -> OptionType.PE
            else -> null
        }
    }

    private fun parseDate(
        value: String,
        pattern: String
    ): String? {

        return try {

            val input =
                SimpleDateFormat(
                    pattern,
                    Locale.US
                ).apply {
                    isLenient = false
                }

            val output =
                SimpleDateFormat(
                    "dd-MMM-yyyy",
                    Locale.US
                )

            output.format(
                input.parse(value)
                    ?: return null
            )

        } catch (_: Exception) {
            null
        }
    }

    private fun getValue(
        values: List<String>,
        indexMap: Map<String, Int>,
        vararg names: String
    ): String? {

        for (name in names) {

            val index =
                indexMap[name.uppercase(Locale.US)]
                    ?: continue

            if (index < values.size) {
                return values[index]
            }
        }

        return null
    }

    private fun splitCsvLine(
        line: String
    ): List<String> {

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
                    result += current.toString()
                    current.clear()
                }

                else -> {
                    current.append(char)
                }
            }

            index++
        }

        result += current.toString()

        return result
    }
}
