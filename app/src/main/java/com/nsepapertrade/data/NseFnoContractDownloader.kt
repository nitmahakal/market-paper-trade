package com.nsepapertrade.data

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

class NseFnoContractDownloader {


companion object {
    private const val NSE_HOME_URL =
        "https://www.nseindia.com/"

    private const val NSE_CONTRACT_URL =
        "https://nsearchives.nseindia.com/content/fo/NSE_FO_contract_"

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"
}

suspend fun download(
    date: String
): List<String> {

    val cookies = loadNseCookies()

    val url = URL(
        NSE_CONTRACT_URL + date + ".csv.gz"
    )

    val connection =
        url.openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 45_000

        connection.setRequestProperty(
            "User-Agent",
            USER_AGENT
        )

        connection.setRequestProperty(
            "Accept",
            "text/html,application/xhtml+xml," +
                "application/xml;q=0.9,*/*;q=0.8"
        )

        connection.setRequestProperty(
            "Accept-Language",
            "en-US,en;q=0.9"
        )

        connection.setRequestProperty(
            "Referer",
            NSE_HOME_URL
        )

        if (cookies.isNotBlank()) {
            connection.setRequestProperty(
                "Cookie",
                cookies
            )
        }

        val responseCode =
            connection.responseCode
        
        val contentType =
            connection.contentType ?: "unknown"
        
        if (responseCode !in 200..299) {
            throw IllegalStateException(
                "NSE contract download failed: " +
                    "HTTP $responseCode " +
                    "type=$contentType"
            )
        }
        
        val inputStream =
            connection.inputStream
        
        val pushbackStream =
            java.io.PushbackInputStream(
                inputStream,
                2
            )
        
        val firstByte =
            pushbackStream.read()
        
        val secondByte =
            pushbackStream.read()
        
        if (firstByte < 0 || secondByte < 0) {
            throw IllegalStateException(
                "NSE contract download returned empty response " +
                    "HTTP $responseCode type=$contentType"
            )
        }
        
        pushbackStream.unread(secondByte)
        pushbackStream.unread(firstByte)
        
        val gzipDetected =
            firstByte == 0x1f &&
                secondByte == 0x8b
        
        if (!gzipDetected) {
            throw IllegalStateException(
                "NSE contract response is not GZIP: " +
                    "HTTP $responseCode " +
                    "type=$contentType " +
                    "bytes=$firstByte,$secondByte"
            )
        }
        
        GZIPInputStream(
            pushbackStream
        ).use { gzipStream ->

            BufferedReader(
                InputStreamReader(
                    gzipStream,
                    Charsets.UTF_8
                )
            ).use { reader ->

                return reader.readLines()
            }
        }

    } finally {
        connection.disconnect()
    }
}

private fun loadNseCookies(): String {

    val connection =
        URL(NSE_HOME_URL)
            .openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 20_000

        connection.setRequestProperty(
            "User-Agent",
            USER_AGENT
        )

        connection.setRequestProperty(
            "Accept",
            "text/html,application/xhtml+xml," +
                "application/xml;q=0.9,*/*;q=0.8"
        )

        connection.setRequestProperty(
            "Accept-Language",
            "en-US,en;q=0.9"
        )

        connection.responseCode

        return connection.headerFields
            .filterKeys {
                it.equals(
                    "Set-Cookie",
                    ignoreCase = true
                )
            }
            .values
            .flatten()
            .mapNotNull { cookie ->
                cookie
                    ?.substringBefore(";")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }
            .joinToString("; ")

    } finally {
        connection.disconnect()
    }
}


}
