package com.nsepapertrade.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class NseSpanMarginParser {

    fun parse(
        lines: List<String>
    ): List<NseSpanMarginRecord> {

        if (lines.isEmpty()) {
            return emptyList()
        }

        val xml =
            lines.joinToString("\n")

        val parser =
            Xml.newPullParser().apply {
                setInput(
                    StringReader(xml)
                )
            }

        val records =
            mutableListOf<NseSpanMarginRecord>()

        var event =
            parser.eventType

        while (
            event != XmlPullParser.END_DOCUMENT
        ) {

            if (
                event == XmlPullParser.START_TAG &&
                parser.name == "futPf"
            ) {

                records.addAll(
                    parseFuturePortfolio(parser)
                )

            } else if (
                event == XmlPullParser.START_TAG &&
                parser.name == "oopPf"
            ) {

                records.addAll(
                    parseOptionPortfolio(parser)
                )
            }

            event =
                parser.next()
        }

        return records
    }

    private fun parseFuturePortfolio(
        parser: XmlPullParser
    ): List<NseSpanMarginRecord> {

        var underlying = ""
        var expiry = ""
        var contractId = ""
        var price = 0.0
        var optionType = "NONE"
        var riskArray =
            emptyList<Double>()

        val records =
            mutableListOf<NseSpanMarginRecord>()

        var depth =
            parser.depth

        while (true) {

            when (parser.next()) {

                XmlPullParser.START_TAG -> {

                    when (parser.name) {

                        "pfCode" ->
                            underlying =
                                parser.readText()

                        "fut" -> {

                            expiry = ""
                            contractId = ""
                            price = 0.0
                            riskArray =
                                emptyList()

                            parseFuture(
                                parser
                            ) { data ->
                                expiry =
                                    data.expiry

                                contractId =
                                    data.contractId

                                price =
                                    data.price

                                riskArray =
                                    data.riskArray
                            }

                            if (
                                underlying.isNotBlank() &&
                                riskArray.size == 16
                            ) {

                                records.add(
                                    buildRecord(
                                        underlying =
                                            underlying,
                                        expiry =
                                            expiry,
                                        contractType =
                                            "FUTURE",
                                        optionType =
                                            optionType,
                                        strikePrice =
                                            0.0,
                                        contractId =
                                            contractId,
                                        price =
                                            price,
                                        riskArray =
                                            riskArray
                                    )
                                )
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (
                        parser.depth == depth &&
                        parser.name == "futPf"
                    ) {
                        break
                    }
                }

                XmlPullParser.END_DOCUMENT ->
                    break
            }
        }

        return records
    }

    private fun parseOptionPortfolio(
        parser: XmlPullParser
    ): List<NseSpanMarginRecord> {

        var underlying = ""

        val records =
            mutableListOf<NseSpanMarginRecord>()

        while (true) {

            when (parser.next()) {

                XmlPullParser.START_TAG -> {

                    when (parser.name) {

                        "pfCode" ->
                            underlying =
                                parser.readText()

                        "series" -> {

                            records.addAll(
                                parseOptionSeries(
                                    parser,
                                    underlying
                                )
                            )
                        }
                    }
                }

                XmlPullParser.END_TAG -> {

                    if (
                        parser.name == "oopPf"
                    ) {
                        break
                    }
                }

                XmlPullParser.END_DOCUMENT ->
                    break
            }
        }

        return records
    }

    private fun parseOptionSeries(
        parser: XmlPullParser,
        underlying: String
    ): List<NseSpanMarginRecord> {

        var expiry = ""

        val records =
            mutableListOf<NseSpanMarginRecord>()

        while (true) {

            when (parser.next()) {

                XmlPullParser.START_TAG -> {

                    when (parser.name) {

                        "pe" ->
                            expiry =
                                parser.readText()

                        "opt" -> {

                            val option =
                                parseOption(
                                    parser
                                )

                            if (
                                underlying.isNotBlank() &&
                                expiry.isNotBlank() &&
                                option.riskArray.size == 16
                            ) {

                                records.add(
                                    buildRecord(
                                        underlying =
                                            underlying,
                                        expiry =
                                            expiry,
                                        contractType =
                                            "OPTION",
                                        optionType =
                                            option.optionType,
                                        strikePrice =
                                            option.strike,
                                        contractId =
                                            option.contractId,
                                        price =
                                            option.price,
                                        riskArray =
                                            option.riskArray
                                    )
                                )
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {

                    if (
                        parser.name == "series"
                    ) {
                        break
                    }
                }

                XmlPullParser.END_DOCUMENT ->
                    break
            }
        }

        return records
    }

    private fun parseFuture(
        parser: XmlPullParser,
        onParsed:
            (ParsedContract) -> Unit
    ) {

        var expiry = ""
        var contractId = ""
        var price = 0.0
        var riskArray =
            emptyList<Double>()

        while (true) {

            when (parser.next()) {

                XmlPullParser.START_TAG -> {

                    when (parser.name) {

                        "cId" ->
                            contractId =
                                parser.readText()

                        "pe" ->
                            expiry =
                                parser.readText()

                        "p" ->
                            price =
                                parser.readDouble()

                        "ra" ->
                            riskArray =
                                parseRiskArray(
                                    parser
                                )
                    }
                }

                XmlPullParser.END_TAG -> {

                    if (
                        parser.name == "fut"
                    ) {

                        onParsed(
                            ParsedContract(
                                contractId =
                                    contractId,
                                expiry =
                                    expiry,
                                price =
                                    price,
                                strike =
                                    0.0,
                                optionType =
                                    "NONE",
                                riskArray =
                                    riskArray
                            )
                        )

                        break
                    }
                }

                XmlPullParser.END_DOCUMENT ->
                    return
            }
        }
    }

    private fun parseOption(
        parser: XmlPullParser
    ): ParsedContract {

        var contractId = ""
        var price = 0.0
        var strike = 0.0
        var optionType = "NONE"
        var riskArray =
            emptyList<Double>()

        while (true) {

            when (parser.next()) {

                XmlPullParser.START_TAG -> {

                    when (parser.name) {

                        "cId" ->
                            contractId =
                                parser.readText()

                        "p" ->
                            price =
                                parser.readDouble()

                        "k" ->
                            strike =
                                parser.readDouble()

                        "o" ->
                            optionType =
                                parser.readText()

                        "ra" ->
                            riskArray =
                                parseRiskArray(
                                    parser
                                )
                    }
                }

                XmlPullParser.END_TAG -> {

                    if (
                        parser.name == "opt"
                    ) {

                        return ParsedContract(
                            contractId =
                                contractId,
                            expiry = "",
                            price =
                                price,
                            strike =
                                strike,
                            optionType =
                                optionType,
                            riskArray =
                                riskArray
                        )
                    }
                }

                XmlPullParser.END_DOCUMENT ->
                    return ParsedContract(
                        contractId =
                            contractId,
                        expiry = "",
                        price =
                            price,
                        strike =
                            strike,
                        optionType =
                            optionType,
                        riskArray =
                            riskArray
                    )
            }
        }
    }

    private fun parseRiskArray(
        parser: XmlPullParser
    ): List<Double> {

        val values =
            mutableListOf<Double>()

        while (true) {

            when (parser.next()) {

                XmlPullParser.START_TAG -> {

                    if (
                        parser.name == "a"
                    ) {

                        values.add(
                            parser.readDouble()
                        )
                    }
                }

                XmlPullParser.END_TAG -> {

                    if (
                        parser.name == "ra"
                    ) {
                        break
                    }
                }

                XmlPullParser.END_DOCUMENT ->
                    break
            }
        }

        return values
    }

    private fun buildRecord(
        underlying: String,
        expiry: String,
        contractType: String,
        optionType: String,
        strikePrice: Double,
        contractId: String,
        price: Double,
        riskArray: List<Double>
    ): NseSpanMarginRecord {

        val normalizedOption =
            when (
                optionType.uppercase()
            ) {

                "C" -> "CE"
                "P" -> "PE"
                else -> "NONE"
            }

        val contractSymbol =
            if (
                contractType == "FUTURE"
            ) {

                "${underlying}_${expiry}_FUT"

            } else {

                "${underlying}_${expiry}_" +
                    "${formatStrike(strikePrice)}_" +
                    normalizedOption
            }

        return NseSpanMarginRecord(
            contractSymbol =
                contractSymbol,
            underlying =
                underlying.uppercase(),
            expiry =
                expiry,
            strikePrice =
                strikePrice,
            optionType =
                normalizedOption,
            contractType =
                contractType,
            lotSize = 1,
            scanRange = 0.0,
            volatilityScanRange = 0.0,
            riskArray =
                riskArray,
            timestamp =
                System.currentTimeMillis()
        )
    }

    private fun formatStrike(
        value: Double
    ): String {

        return if (
            value % 1.0 == 0.0
        ) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    private fun XmlPullParser.readText():
        String {

        return nextText().trim()
    }

    private fun XmlPullParser.readDouble():
        Double {

        return readText()
            .toDoubleOrNull()
            ?: 0.0
    }

    private data class ParsedContract(
        val contractId: String,
        val expiry: String,
        val price: Double,
        val strike: Double,
        val optionType: String,
        val riskArray: List<Double>
    )
}
