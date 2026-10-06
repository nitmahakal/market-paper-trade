package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

class NseFnoMarketDataProvider :
    FnoMarketDataProvider {

    companion object {
        private const val BASE_URL =
            "https://www.nseindia.com"

        private const val QUOTE_URL =
            "$BASE_URL/api/quote-derivative?symbol="

        private const val PAGE_URL =
            "$BASE_URL/get-quotes/derivatives?symbol="

        private const val TIMEOUT_MS = 10_000
    }

    override suspend fun getQuote(
        contract: FnoContract
    ): MarketQuote? {

        val symbol =
            URLEncoder.encode(
                contract.underlying.uppercase(),
                "UTF-8"
            )

        val pageConnection =
            URL(
                PAGE_URL + symbol
            ).openConnection()
                as HttpURLConnection

        try {

            pageConnection.requestMethod = "GET"
            pageConnection.connectTimeout =
                TIMEOUT_MS
            pageConnection.readTimeout =
                TIMEOUT_MS

            pageConnection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0"
            )

            pageConnection.setRequestProperty(
                "Accept",
                "text/html,application/xhtml+xml"
            )

            pageConnection.inputStream
                .bufferedReader()
                .use {
                    it.readText()
                }

        } finally {

            pageConnection.disconnect()
        }

        val apiConnection =
            URL(
                QUOTE_URL + symbol
            ).openConnection()
                as HttpURLConnection

        return try {

            apiConnection.requestMethod = "GET"
            apiConnection.connectTimeout =
                TIMEOUT_MS
            apiConnection.readTimeout =
                TIMEOUT_MS

            apiConnection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0"
            )

            apiConnection.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (
                apiConnection.responseCode
                !in 200..299
            ) {
                return null
            }

            val response =
                apiConnection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            findContractPrice(
                response = response,
                contract = contract
            )

        } finally {

            apiConnection.disconnect()
        }
    }

    private fun findContractPrice(
        response: String,
        contract: FnoContract
    ): MarketQuote? {

        val root =
            JSONObject(response)

        val stocks =
            root.optJSONArray("stocks")
                ?: return null

        for (
            index in 0 until stocks.length()
        ) {

            val item =
                stocks.optJSONObject(index)
                    ?: continue

            val metadata =
                item.optJSONObject("metadata")
                    ?: continue

            val instrumentType =
                metadata.optString(
                    "instrumentType"
                )

            val expiry =
                metadata.optString(
                    "expiryDate"
                )

            val strike =
                metadata.optDouble(
                    "strikePrice",
                    0.0
                )

            val optionType =
                metadata.optString(
                    "optionType"
                )

            val matchesType =
                when (contract.contractType) {

                    FnoContractType.FUTURE ->
                        instrumentType
                            .contains(
                                "Future",
                                ignoreCase = true
                            )

                    FnoContractType.OPTION ->
                        instrumentType
                            .contains(
                                "Option",
                                ignoreCase = true
                            )
                }

            if (!matchesType) {
                continue
            }

            if (
                !expiryMatches(
                    expiry,
                    contract.expiry
                )
            ) {
                continue
            }

            if (
                contract.contractType ==
                FnoContractType.OPTION
            ) {

                if (
                    kotlin.math.abs(
                        strike -
                            contract.strikePrice
                    ) > 0.001
                ) {
                    continue
                }

                if (
                    !optionType.equals(
                        contract.optionType.name,
                        ignoreCase = true
                    )
                ) {
                    continue
                }
            }

            val price =
                firstPositive(
                    metadata,
                    "lastPrice",
                    "ltp",
                    "closePrice"
                )

            if (price != null) {

                return MarketQuote(
                    symbol = contract.symbol,
                    price = price,
                    timestamp =
                        System.currentTimeMillis(),
                    source =
                        MarketDataSource.LIVE
                )
            }
        }

        return null
    }

    private fun expiryMatches(
        apiExpiry: String,
        contractExpiry: String
    ): Boolean {

        return apiExpiry
            .trim()
            .equals(
                contractExpiry.trim(),
                ignoreCase = true
            )
    }

    private fun firstPositive(
        metadata: JSONObject,
        vararg keys: String
    ): Double? {

        for (key in keys) {

            if (
                metadata.has(key) &&
                !metadata.isNull(key)
            ) {

                val value =
                    metadata.optDouble(
                        key,
                        0.0
                    )

                if (value > 0.0) {
                    return value
                }
            }
        }

        return null
    }
}
