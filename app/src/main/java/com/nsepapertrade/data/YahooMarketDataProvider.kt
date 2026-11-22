package com.nsepapertrade.data

import com.nsepapertrade.model.Instrument
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class YahooMarketDataProvider : MarketDataProvider {

    companion object {

        private val HOSTS = listOf(
            "query1.finance.yahoo.com",
            "query2.finance.yahoo.com"
        )

        private const val TIMEOUT_MS = 8_000

        // Quote older than this is shown as DELAYED
        // (market closed, weekend, holiday or lagging feed).
        private const val FRESH_LIMIT_MS = 2 * 60_000L

        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.0.0 Safari/537.36"
    }

    override suspend fun getQuote(
        instrument: Instrument
    ): MarketQuote? {

        val yahooSymbol = URLEncoder.encode(
            "${instrument.symbol}.NS",
            "UTF-8"
        )

        for (host in HOSTS) {

            val body = fetch(
                host = host,
                yahooSymbol = yahooSymbol
            ) ?: continue

            val quote = parseQuote(
                body = body,
                instrument = instrument
            )

            if (quote != null) {
                return quote
            }
        }

        return null
    }

    private fun fetch(
        host: String,
        yahooSymbol: String
    ): String? {

        val url = URL(
            "https://$host/v8/finance/chart/" +
                "$yahooSymbol?range=1d&interval=1d" +
                "&includePrePost=false"
        )

        val connection =
            url.openConnection() as HttpURLConnection

        return try {

            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty(
                "User-Agent",
                USER_AGENT
            )
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (connection.responseCode !in 200..299) {
                null
            } else {
                connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }
            }

        } catch (_: IOException) {

            null

        } finally {

            connection.disconnect()
        }
    }

    private fun parseQuote(
        body: String,
        instrument: Instrument
    ): MarketQuote? {

        return try {

            val results =
                JSONObject(body)
                    .optJSONObject("chart")
                    ?.optJSONArray("result")

            if (results == null || results.length() == 0) {
                return null
            }

            val result =
                results.optJSONObject(0)
                    ?: return null

            val meta =
                result.optJSONObject("meta")
                    ?: return null

            val price =
                if (meta.has("regularMarketPrice") &&
                    !meta.isNull("regularMarketPrice")
                ) {
                    meta.optDouble(
                        "regularMarketPrice",
                        0.0
                    )
                } else {
                    lastValue(result, "close")
                }

            if (price == null || price <= 0.0) {
                return null
            }

            val now = System.currentTimeMillis()

            val marketTimeSeconds =
                meta.optLong("regularMarketTime", 0L)

            val marketTimeMs =
                if (marketTimeSeconds > 0L) {
                    marketTimeSeconds * 1000L
                } else {
                    now
                }

            val source =
                if (now - marketTimeMs <= FRESH_LIMIT_MS) {
                    MarketDataSource.LIVE
                } else {
                    MarketDataSource.DELAYED
                }

            MarketQuote(
                symbol = instrument.symbol,
                price = price,
                timestamp = marketTimeMs,
                source = source,
                openPrice = lastValue(result, "open") ?: 0.0
            )

        } catch (_: JSONException) {

            null
        }
    }

    // Last non-null value of indicators.quote[0].<field>
    private fun lastValue(
        result: JSONObject,
        field: String
    ): Double? {

        val values =
            result
                .optJSONObject("indicators")
                ?.optJSONArray("quote")
                ?.optJSONObject(0)
                ?.optJSONArray(field)
                ?: return null

        var last: Double? = null

        for (i in 0 until values.length()) {
            if (!values.isNull(i)) {
                last = values.getDouble(i)
            }
        }

        return last
    }
}
