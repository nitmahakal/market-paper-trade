package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract
import com.nsepapertrade.model.FnoContractType
import com.nsepapertrade.model.FnoUnderlyingType
import com.nsepapertrade.model.OptionType
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

private class CachedResponse(
    val body: String,
    val time: Long
)

class NseFnoMarketDataProvider :
    FnoMarketDataProvider,
    FnoOptionChainProvider {

    companion object {

        private const val BASE_URL =
            "https://www.nseindia.com"

        private const val QUOTE_URL =
            "$BASE_URL/api/NextApi/apiClient/GetQuoteApi" +
                "?functionName=getSymbolDerivativesData&symbol="

        private const val OPTION_CHAIN_URL =
            "$BASE_URL/api/option-chain-v3?type="

        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"

        private const val TIMEOUT_MS = 10_000
        private const val COOKIE_TTL_MS = 4 * 60_000L
        private const val QUOTE_CACHE_MS = 3_000L
        private const val CHAIN_CACHE_MS = 8_000L

        private val lock = Any()
        private var cookieHeader: String? = null
        private var cookieTime = 0L
        private val responseCache =
            HashMap<String, CachedResponse>()

        // Lot size per underlying (filled from the stored contract
        // list). The v3 chain may not carry lot sizes.
        @Volatile
        private var lotSizes: Map<String, Int> = emptyMap()

        fun setLotSizes(sizes: Map<String, Int>) {
            lotSizes = sizes
        }

        private val EXPIRY_PATTERNS = listOf(
            "dd-MMM-yyyy",
            "ddMMMyyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd"
        )

        // NSE wants the expiry as 25-Nov-2026.
        fun normalizeExpiry(expiry: String): String {

            val text = expiry.trim()

            for (pattern in EXPIRY_PATTERNS) {
                try {
                    val parser =
                        SimpleDateFormat(pattern, Locale.US)

                    parser.isLenient = false

                    val date = parser.parse(text) ?: continue

                    return SimpleDateFormat(
                        "dd-MMM-yyyy",
                        Locale.US
                    ).format(date)

                } catch (_: Exception) {
                    // try the next format
                }
            }

            return text
        }
    }

    override suspend fun getQuote(
        contract: FnoContract
    ): MarketQuote? {

        val symbol = encode(contract.underlying)

        var primary: MarketQuote? = null
        var failure: Exception? = null

        try {
            val response = fetchJson(
                url = QUOTE_URL + symbol,
                cacheMs = QUOTE_CACHE_MS
            )

            primary = findContractPrice(
                response = response,
                contract = contract
            )
        } catch (e: IOException) {
            failure = e
        }

        if (primary != null) {
            return primary
        }

        // Options: the option chain has every strike's last price.
        if (contract.contractType == FnoContractType.OPTION) {
            try {
                val fromChain = quoteFromChain(contract)

                if (fromChain != null) {
                    return fromChain
                }
            } catch (e: IOException) {
                if (failure == null) {
                    failure = e
                }
            }
        }

        if (failure != null) {
            throw failure
        }

        return null
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

        val response = fetchJson(
            url = chainUrl(normalizedUnderlying, expiry),
            cacheMs = CHAIN_CACHE_MS
        )

        return parseOptionChain(
            response = response,
            underlying = normalizedUnderlying,
            expiry = expiry
        )
    }

    private fun chainUrl(
        underlying: String,
        expiry: String
    ): String {

        val type =
            if (isIndexUnderlying(underlying)) {
                "Indices"
            } else {
                "Equity"
            }

        return OPTION_CHAIN_URL + type +
            "&symbol=" + encode(underlying) +
            "&expiry=" + encode(normalizeExpiry(expiry))
    }

    private fun quoteFromChain(
        contract: FnoContract
    ): MarketQuote? {

        val response = fetchJson(
            url = chainUrl(
                contract.underlying.uppercase(),
                contract.expiry
            ),
            cacheMs = CHAIN_CACHE_MS
        )

        val chain = parseOptionChain(
            response = response,
            underlying = contract.underlying.uppercase(),
            expiry = contract.expiry
        ) ?: return null

        val row = chain.rows.firstOrNull {
            kotlin.math.abs(
                it.strikePrice - contract.strikePrice
            ) < 0.001
        } ?: return null

        val option =
            if (contract.optionType == OptionType.CE) {
                row.call
            } else {
                row.put
            }

        val price = option?.ltp ?: 0.0

        if (price <= 0.0) {
            return null
        }

        return MarketQuote(
            symbol = contract.symbol,
            price = price,
            timestamp = System.currentTimeMillis(),
            source = MarketDataSource.LIVE
        )
    }

    // ---------------------------------------------------------
    // Network: shared cookie cache, retry on block, clear errors
    // ---------------------------------------------------------

    private fun fetchJson(
        url: String,
        cacheMs: Long
    ): String {

        val now = System.currentTimeMillis()

        synchronized(lock) {
            val cached = responseCache[url]
            if (cached != null &&
                now - cached.time < cacheMs
            ) {
                return cached.body
            }
        }

        var result = requestWithCookies(
            url = url,
            forceNewCookies = false
        )

        if (result.first == 401 || result.first == 403) {
            result = requestWithCookies(
                url = url,
                forceNewCookies = true
            )
        }

        val code = result.first
        val body = result.second

        when {
            code in 200..299 -> Unit

            code == 401 || code == 403 ->
                throw IOException(
                    "NSE blocked the request (HTTP $code). " +
                        "Wait a minute and try again."
                )

            code == 429 ->
                throw IOException(
                    "NSE rate limit reached (HTTP 429). " +
                        "Try again after some time."
                )

            else ->
                throw IOException(
                    "NSE server error (HTTP $code)."
                )
        }

        val trimmed = body.trim()

        if (trimmed.isEmpty() || trimmed == "{}") {
            throw IOException(
                "NSE returned empty data " +
                    "(session rejected). Try again in a minute."
            )
        }

        if (!trimmed.startsWith("{") &&
            !trimmed.startsWith("[")
        ) {
            throw IOException(
                "NSE returned an invalid response " +
                    "(blocked or page changed)."
            )
        }

        synchronized(lock) {
            responseCache[url] = CachedResponse(
                body = body,
                time = System.currentTimeMillis()
            )
        }

        return body
    }

    private fun requestWithCookies(
        url: String,
        forceNewCookies: Boolean
    ): Pair<Int, String> {

        return try {
            val cookies = getCookies(forceNewCookies)
            requestOnce(url, cookies)
        } catch (e: IOException) {
            throw IOException(
                e.message
                    ?: "Cannot reach NSE. " +
                    "Check your internet connection.",
                e
            )
        }
    }

    private fun getCookies(
        forceRefresh: Boolean
    ): String? {

        synchronized(lock) {

            val now = System.currentTimeMillis()

            if (!forceRefresh &&
                !cookieHeader.isNullOrBlank() &&
                now - cookieTime < COOKIE_TTL_MS
            ) {
                return cookieHeader
            }

            val fresh = fetchCookies()

            if (!fresh.isNullOrBlank()) {
                cookieHeader = fresh
                cookieTime = now
            }

            return fresh
        }
    }

    private fun fetchCookies(): String? {

        val connection =
            URL("$BASE_URL/")
                .openConnection()
                as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            connection.setRequestProperty(
                "User-Agent",
                USER_AGENT
            )
            connection.setRequestProperty(
                "Accept",
                "text/html,application/xhtml+xml"
            )
            connection.setRequestProperty(
                "Accept-Language",
                "en-US,en;q=0.9"
            )

            val code = connection.responseCode

            val stream =
                if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            stream?.bufferedReader()?.use { it.readText() }

            return connection.headerFields.entries
                .filter {
                    it.key?.equals(
                        "Set-Cookie",
                        ignoreCase = true
                    ) == true
                }
                .flatMap { it.value }
                .mapNotNull { cookie ->
                    cookie.substringBefore(";")
                        .takeIf { it.isNotBlank() }
                }
                .joinToString("; ")
                .takeIf { it.isNotBlank() }

        } finally {
            connection.disconnect()
        }
    }

    private fun requestOnce(
        url: String,
        cookies: String?
    ): Pair<Int, String> {

        val connection =
            URL(url)
                .openConnection()
                as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS

            connection.setRequestProperty(
                "User-Agent",
                USER_AGENT
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

            val code = connection.responseCode

            val stream =
                if (code in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

            val text =
                stream?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

            return code to text

        } finally {
            connection.disconnect()
        }
    }

    // ---------------------------------------------------------
    // Parsing
    // ---------------------------------------------------------

    private fun findContractPrice(
        response: String,
        contract: FnoContract
    ): MarketQuote? {

        val root = parseRoot(response)

        val data =
            root.optJSONArray("data")
                ?: return null

        for (index in 0 until data.length()) {

            val item =
                data.optJSONObject(index)
                    ?: continue

            if (!item.optString("underlying")
                    .equals(
                        contract.underlying,
                        ignoreCase = true
                    )
            ) {
                continue
            }

            val instrumentType =
                item.optString("instrumentType")

            val matchesType =
                when (contract.contractType) {

                    FnoContractType.FUTURE ->
                        instrumentType.equals(
                            "FUTSTK",
                            ignoreCase = true
                        ) || instrumentType.equals(
                            "FUTIDX",
                            ignoreCase = true
                        )

                    FnoContractType.OPTION ->
                        instrumentType.equals(
                            "OPTSTK",
                            ignoreCase = true
                        ) || instrumentType.equals(
                            "OPTIDX",
                            ignoreCase = true
                        )
                }

            if (!matchesType) {
                continue
            }

            if (!expiryMatches(
                    item.optString("expiryDate"),
                    contract.expiry
                )
            ) {
                continue
            }

            if (contract.contractType ==
                FnoContractType.OPTION
            ) {

                val strike =
                    item.optDouble("strikePrice", 0.0)

                if (kotlin.math.abs(
                        strike - contract.strikePrice
                    ) > 0.001
                ) {
                    continue
                }

                if (!item.optString("optionType")
                        .equals(
                            contract.optionType.name,
                            ignoreCase = true
                        )
                ) {
                    continue
                }
            }

            val price =
                firstPositive(
                    item,
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
                    source = MarketDataSource.LIVE,
                    openPrice = positiveOrZero(
                        item,
                        "openPrice"
                    )
                )
            }
        }

        return null
    }

    // Reads the v3 chain. Field names are read loosely, and the
    // requested expiry is used for every contract, so the symbols
    // match the stored contract list exactly.
    private fun parseOptionChain(
        response: String,
        underlying: String,
        expiry: String
    ): FnoOptionChain? {

        val root = parseRoot(response)

        val data = findChainData(root) ?: return null

        val wantedExpiry = normalizeExpiry(expiry)

        val items = ArrayList<JSONObject>()

        for (index in 0 until data.length()) {
            data.optJSONObject(index)?.let { items.add(it) }
        }

        // If the reply says which expiry each row belongs to and some
        // match, keep only those; otherwise trust the request.
        val anyExpiryMatch = items.any {
            normalizeExpiry(itemExpiry(it)) == wantedExpiry
        }

        val rows =
            linkedMapOf<
                Double,
                MutableList<FnoOptionChainContract>
            >()

        for (item in items) {

            if (anyExpiryMatch &&
                normalizeExpiry(itemExpiry(item)) != wantedExpiry
            ) {
                continue
            }

            val strike = strikeOf(item)

            if (strike.isNaN()) {
                continue
            }

            val call =
                parseOptionContract(
                    item = item,
                    underlying = underlying,
                    expiry = expiry,
                    strike = strike,
                    optionType = OptionType.CE
                )

            val put =
                parseOptionContract(
                    item = item,
                    underlying = underlying,
                    expiry = expiry,
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
                .filter { it.value.isNotEmpty() }
                .sortedBy { it.key }
                .map { entry ->

                    FnoOptionChainRow(
                        strikePrice = entry.key,
                        call = entry.value.firstOrNull {
                            it.contract.optionType ==
                                OptionType.CE
                        },
                        put = entry.value.firstOrNull {
                            it.contract.optionType ==
                                OptionType.PE
                        }
                    )
                }

        if (chainRows.isEmpty()) {
            return null
        }

        return FnoOptionChain(
            underlying = underlying,
            expiry = expiry,
            rows = chainRows,
            timestamp = System.currentTimeMillis(),
            underlyingValue = findUnderlyingValue(root, data)
        )
    }

    // The stock / index price NSE sends along with the chain.
    private fun findUnderlyingValue(
        root: JSONObject,
        data: JSONArray
    ): Double {

        val direct =
            root.optJSONObject("records")
                ?.optDouble("underlyingValue", 0.0)
                ?: 0.0

        if (direct > 0.0) {
            return direct
        }

        for (index in 0 until data.length()) {

            val item = data.optJSONObject(index) ?: continue

            for (side in listOf("CE", "PE")) {

                val value =
                    item.optJSONObject(side)
                        ?.optDouble("underlyingValue", 0.0)
                        ?: 0.0

                if (value > 0.0) {
                    return value
                }
            }
        }

        return 0.0
    }

    private fun findChainData(
        root: JSONObject
    ): JSONArray? {

        root.optJSONObject("records")
            ?.optJSONArray("data")
            ?.let { return it }

        root.optJSONObject("filtered")
            ?.optJSONArray("data")
            ?.let { return it }

        return root.optJSONArray("data")
    }

    private fun itemExpiry(item: JSONObject): String {

        for (key in listOf("expiryDates", "expiryDate")) {
            val value = item.optString(key, "")

            if (value.isNotBlank()) {
                return value
            }
        }

        for (side in listOf("CE", "PE")) {
            val inner = item.optJSONObject(side) ?: continue

            for (key in listOf("expiryDates", "expiryDate")) {
                val value = inner.optString(key, "")

                if (value.isNotBlank()) {
                    return value
                }
            }
        }

        return ""
    }

    private fun strikeOf(item: JSONObject): Double {

        if (item.has("strikePrice") &&
            !item.isNull("strikePrice")
        ) {
            return item.optDouble("strikePrice", Double.NaN)
        }

        for (side in listOf("CE", "PE")) {
            val inner = item.optJSONObject(side) ?: continue

            if (inner.has("strikePrice") &&
                !inner.isNull("strikePrice")
            ) {
                return inner.optDouble(
                    "strikePrice",
                    Double.NaN
                )
            }
        }

        return Double.NaN
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

        var lotSize =
            optionData.optInt(
                "marketLot",
                item.optInt("marketLot", 0)
            )

        if (lotSize <= 0) {
            lotSize = lotSizes[underlying] ?: 1
        }

        val contract =
            FnoContract(
                underlying = underlying,
                underlyingType =
                    if (isIndexUnderlying(underlying)) {
                        FnoUnderlyingType.INDEX
                    } else {
                        FnoUnderlyingType.STOCK
                    },
                contractType = FnoContractType.OPTION,
                expiry = expiry,
                strikePrice = strike,
                optionType = optionType,
                lotSize = lotSize
            )

        return FnoOptionChainContract(
            contract = contract,
            ltp = positiveOrZero(
                optionData,
                "lastPrice"
            ),
            bidPrice = firstPositive(
                optionData,
                "bidprice",
                "bidPrice"
            ) ?: 0.0,
            askPrice = firstPositive(
                optionData,
                "askPrice",
                "askprice"
            ) ?: 0.0,
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

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private fun parseRoot(
        response: String
    ): JSONObject {

        return try {
            JSONObject(response)
        } catch (e: JSONException) {
            throw IOException(
                "NSE sent unreadable data " +
                    "(format may have changed).",
                e
            )
        }
    }

    private fun isIndexUnderlying(
        underlying: String
    ): Boolean {

        return when (underlying.uppercase()) {
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
        json: JSONObject,
        vararg keys: String
    ): Double? {

        for (key in keys) {

            if (json.has(key) && !json.isNull(key)) {

                val value =
                    json.optDouble(key, 0.0)

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
            json.optDouble(key, 0.0)

        return if (value > 0.0) value else 0.0
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
