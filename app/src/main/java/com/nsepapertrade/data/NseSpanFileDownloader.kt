package com.nsepapertrade.data

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class NseSpanFileDownloader {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
    }

    suspend fun download(
        urlString: String
    ): List<String> {

        require(urlString.isNotBlank()) {
            "SPAN file URL cannot be blank."
        }

        val connection =
            URL(urlString)
                .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout =
                CONNECT_TIMEOUT_MS
            connection.readTimeout =
                READ_TIMEOUT_MS

            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0"
            )

            connection.setRequestProperty(
                "Accept",
                "*/*"
            )

            val responseCode =
                connection.responseCode

            require(responseCode in 200..299) {
                "NSE SPAN download failed: HTTP $responseCode"
            }

            ZipInputStream(
                connection.inputStream
            ).use { zipStream ->

                while (true) {

                    val entry =
                        zipStream.nextEntry
                            ?: break

                    if (entry.isDirectory) {
                        continue
                    }

                    BufferedReader(
                        InputStreamReader(
                            zipStream,
                            Charsets.UTF_8
                        )
                    ).use { reader ->

                        return reader.readLines()
                    }
                }
            }

            throw IllegalStateException(
                "NSE SPAN ZIP contains no readable file."
            )

        } finally {
            connection.disconnect()
        }
    }
}
