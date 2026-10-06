
package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.FnoUnderlyingType
import com.nsepapertrade.model.OptionType
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

class NseFnoMarketDataProvider :
    FnoMarketDataProvider,
    FnoOptionChainProvider {

    companion object {
        private const val BASE_URL =
            "https://www.nseindia.com"

        private const val QUOTE_URL =
            "$BASE_URL/api/quote-derivative?symbol="

        private const val OPTION_CHAIN_INDEX_URL =
            "$BASE_URL/api/option-chain-indices?symbol="

        private const val OPTION_CHAIN_EQUITY_URL =
            "$BASE_URL/api/option-chain-equities?symbol="

        private const val PAGE_URL =
            "$BASE_URL/get-quotes/derivatives?symbol="

        private const val OPTION_CHAIN_PAGE_URL =
            "$BASE_URL/option-chain?symbol="

        private const val TIMEOUT_MS = 10_000
    }

    override suspend fun getQuote(
        contract: FnoContract
    ): MarketQuote? {

        val symbol = encode(contract.underlying)

        val cookies = loadPageCookies(
            PAGE_URL + symbol
        )

        val response = getJson(
            url = QUOTE_URL + symbol,
            cookies = cookies
        ) ?: return null

        return findContractPrice(
            response = response,
            contract = contract
        )
    }

    override suspend fun getOptionChain(
        underlying: String,
        expiry: String
    ): FnoOptionChain? {

        val normalizedUnderlying =
            underlying.trim().uppercase()

        if (normalizedUnderlying.isEmpty()) {
            return null
        }

        val symbol = encode(normalizedUnderlying)

        val cookies = loadPageCookies(
            OPTION_CHAIN_PAGE_URL + symbol
        )

        val indexUnderlying =
            isIndexUnderlying(normalizedUnderlying)

        val apiUrl =
            if (indexUnderlying) {
                OPTION_CHAIN_INDEX_URL + symbol
            } else {
                OPTION_CHAIN_EQUITY_URL + symbol
            }

        val response =
            getJson(
                url = apiUrl,
                cookies = cookies
            ) ?: return null

        return parseOptionChain(
            response = response,
            underlying = normalizedUnderlying,
            expiry = expiry
        )
    }

    private fun loadPageCookies(
        pageUrl: String
    ): String? {

        val connection =
            URL(pageUrl)
                .openConnection()
                as HttpURLConnection

        return try {

            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0"
            )

            connection.setRequestProperty(
                "Accept",
                "text/html,application/xhtml+xml"
            )

            connection.setRequestProperty(
                "Accept-Language",
                "en-US,en;q=0.9"
            )

            connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            connection.headerFields["Set-Cookie"]
                ?.mapNotNull { cookie ->
                    cookie.substringBefore(";")
                        .takeIf { it.isNotBlank() }
                }
                ?.joinToString("; ")

        } catch (_: Exception) {

            null

        } finally {

            connection.disconnect()
        }
    }

    private fun getJson(
        url: String,
        cookies: String?
    ): String? {

        val connection =
            URL(url)
                .openConnection()
                as HttpURLConnection

        return try {

            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json,text/plain,*/*"
            )

            connection.setRequestProperty(
                "Accept-Language",
                "en-US,en;q=0.9"
            )

            connection.setRequestProperty(
                "Referer",
                BASE_URL
            )

            if (!cookies.isNullOrBlank()) {
                connection.setRequestProperty(
                    "Cookie",
                    cookies
                )
            }

            if (
                connection.responseCode
                !in 200..299
            ) {
                return null
            }

            connection.inputStream
                .bufferedReader()
                .use { it.readText() }

        } catch (_: Exception) {

            null

        } finally {

            connection.disconnect()
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
                        instrumentType.contains(
                            "Future",
                            ignoreCase = true
                        )

                    FnoContractType.OPTION ->
                        instrumentType.contains(
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

    private fun parseOptionChain(
        response: String,
        underlying: String,
        expiry: String
    ): FnoOptionChain? {

        val root =
            JSONObject(response)

        val records =
            root.optJSONObject("records")
                ?: return null

        val data =
            records.optJSONArray("data")
                ?: return null

        val rows =
            linkedMapOf<
                Double,
                MutableList<FnoOptionChainContract>
            >()

        for (
            index in 0 until data.length()
        ) {

            val item =
                data.optJSONObject(index)
                    ?: continue

            val itemExpiry =
                item.optString("expiryDate")

            if (
                !expiryMatches(
                    itemExpiry,
                    expiry
                )
            ) {
                continue
            }

            val strike =
                item.optDouble(
                    "strikePrice",
                    Double.NaN
                )

            if (strike.isNaN()) {
                continue
            }

            val call =
                parseOptionContract(
                    item = item,
                    underlying = underlying,
                    expiry = itemExpiry,
                    strike = strike,
                    optionType = OptionType.CE
                )

            val put =
                parseOptionContract(
                    item = item,
                    underlying = underlying,
                    expiry = itemExpiry,
                    strike = strike,
                    optionType = OptionType.PE
                )

            val contracts =
                rows.getOrPut(strike) {
                    mutableListOf()
                }

            if (call != null) {
                contracts.add(call)
            }

            if (put != null) {
                contracts.add(put)
            }
        }

        val chainRows =
            rows.entries
                .sortedBy { it.key }
                .map { entry ->

                    val call =
                        entry.value.firstOrNull {
                            it.contract.optionType ==
                                OptionType.CE
                        }

                    val put =
                        entry.value.firstOrNull {
                            it.contract.optionType ==
                                OptionType.PE
                        }

                    FnoOptionChainRow(
                        strikePrice = entry.key,
                        call = call,
                        put = put
                    )
                }

        if (chainRows.isEmpty()) {
            return null
        }

        return FnoOptionChain(
            underlying = underlying,
            expiry = expiry,
            rows = chainRows,
            timestamp =
                System.currentTimeMillis()
        )
    }

    private fun parseOptionContract(
        item: JSONObject,
        underlying: String,
        expiry: String,
        strike: Double,
        optionType: OptionType
    ): FnoOptionChainContract? {

        val optionData =
            item.optJSONObject(optionType.name)
                ?: return null

        val contract =
            FnoContract(
                underlying = underlying,
                underlyingType =
                    if (isIndexUnderlying(underlying)) {
                        FnoUnderlyingType.INDEX
                    } else {
                        FnoUnderlyingType.STOCK
                    },
                contractType =
                    FnoContractType.OPTION,
                expiry = expiry,
                strikePrice = strike,
                optionType = optionType,
                lotSize =
                    optionData.optInt(
                        "marketLot",
                        item.optInt(
                            "marketLot",
                            0
                        )
                    )
            )

        if (contract.lotSize <= 0) {
            return null
        }

        return FnoOptionChainContract(
            contract = contract,
            ltp =
                positiveOrZero(
                    optionData,
                    "lastPrice"
                ),
            bidPrice =
                positiveOrZero(
                    optionData,
                    "bidprice"
                ),
            askPrice =
                positiveOrZero(
                    optionData,
                    "askPrice"
                ),
            openInterest =
                optionData.optLong(
                    "openInterest",
                    0L
                ),
            volume =
                optionData.optLong(
                    "totalTradedVolume",
                    0L
                )
        )
    }

    private fun isIndexUnderlying(
        underlying: String
    ): Boolean {

        return when (
            underlying.uppercase()
        ) {
            "NIFTY",
            "BANKNIFTY",
            "FINNIFTY",
            "MIDCPNIFTY",
            "NIFTYNXT50" -> true

            else -> false
        }
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

    private fun positiveOrZero(
        json: JSONObject,
        key: String
    ): Double {

        val value =
            json.optDouble(
                key,
                0.0
            )

        return if (value > 0.0) {
            value
        } else {
            0.0
        }
    }

    private fun encode(
        value: String
    ): String {

        return URLEncoder.encode(
            value,
            "UTF-8"
        )
    }
}

