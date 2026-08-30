package com.nsepapertrade.data

import com.nsepapertrade.model.Instrument
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class YahooMarketDataProvider : MarketDataProvider {

    override suspend fun getQuote(
        instrument: Instrument
    ): MarketQuote? {

        val yahooSymbol = URLEncoder.encode(
            "${instrument.symbol}.NS",
            "UTF-8"
        )

        val url = URL(
            "https://query1.finance.yahoo.com/v8/finance/chart/" +
                "$yahooSymbol?range=1d&interval=1m&includePrePost=false"
        )

        val connection =
            url.openConnection() as HttpURLConnection

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0"
            )

            if (connection.responseCode !in 200..299) {
                return null
            }

            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            val root = JSONObject(response)
            val chart = root.getJSONObject("chart")
            val result = chart
                .getJSONArray("result")
                .getJSONObject(0)

            val meta = result.getJSONObject("meta")

            val price = when {
                meta.has("regularMarketPrice") &&
                    !meta.isNull("regularMarketPrice") ->
                    meta.getDouble("regularMarketPrice")

                else -> {
                    val quote = result
                        .getJSONObject("indicators")
                        .getJSONArray("quote")
                        .getJSONObject(0)

                    val closes = quote.getJSONArray("close")

                    var last: Double? = null

                    for (i in 0 until closes.length()) {
                        if (!closes.isNull(i)) {
                            last = closes.getDouble(i)
                        }
                    }

                    last
                }
            }

            if (price == null || price <= 0.0) {
                null
            } else {
                MarketQuote(
                    symbol = instrument.symbol,
                    price = price,
                    timestamp = System.currentTimeMillis(),
                    source = MarketDataSource.LIVE
                )
            }

        } finally {
            connection.disconnect()
        }
    }
}
