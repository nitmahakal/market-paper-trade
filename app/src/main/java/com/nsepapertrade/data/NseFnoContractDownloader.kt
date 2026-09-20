package com.nsepapertrade.data

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

class NseFnoContractDownloader {

    companion object {
        private const val NSE_CONTRACT_URL =
            "https://nsearchives.nseindia.com/content/fo/NSE_FO_contract_"
    }

    suspend fun download(
        date: String
    ): List<String> {

        val url = URL(
            NSE_CONTRACT_URL + date + ".csv.gz"
        )

        val connection =
            url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0"
            )
            connection.setRequestProperty(
                "Accept",
                "*/*"
            )

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                throw IllegalStateException(
                    "NSE contract download failed: HTTP $responseCode"
                )
            }

            GZIPInputStream(connection.inputStream).use { gzipStream ->
                BufferedReader(
                    InputStreamReader(gzipStream, Charsets.UTF_8)
                ).use { reader ->

                    return reader.readLines()
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
