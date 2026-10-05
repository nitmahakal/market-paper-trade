package com.nsepapertrade.data

class NseSpanRawParser {

    fun parse(
        lines: List<String>
    ): List<NseSpanRawRecord> {

        if (lines.isEmpty()) {
            return emptyList()
        }

        return lines
            .asSequence()
            .map { it.trimEnd('\r', '\n') }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                parseLine(line)
            }
            .toList()
    }

    private fun parseLine(
        line: String
    ): NseSpanRawRecord? {

        val trimmed =
            line.trim()

        if (trimmed.isBlank()) {
            return null
        }

        if (
            trimmed.startsWith("#") ||
            trimmed.startsWith("//")
        ) {
            return null
        }

        val fields =
            when {
                trimmed.contains('|') ->
                    splitDelimited(
                        trimmed,
                        '|'
                    )

                trimmed.contains('\t') ->
                    splitDelimited(
                        trimmed,
                        '\t'
                    )

                looksLikeCsv(trimmed) ->
                    splitDelimited(
                        trimmed,
                        ','
                    )

                looksLikeTaggedRecord(trimmed) ->
                    parseTaggedRecord(
                        trimmed
                    )

                else ->
                    listOf(trimmed)
            }

        if (fields.isEmpty()) {
            return null
        }

        return NseSpanRawRecord(
            fields = fields
        )
    }

    private fun splitDelimited(
        line: String,
        delimiter: Char
    ): List<String> {

        val result =
            mutableListOf<String>()

        val current =
            StringBuilder()

        var insideQuotes = false

        for (character in line) {

            when {

                character == '"' ->
                    insideQuotes =
                        !insideQuotes

                character == delimiter &&
                    !insideQuotes -> {

                    result.add(
                        current
                            .toString()
                            .trim()
                    )

                    current.clear()
                }

                else ->
                    current.append(character)
            }
        }

        result.add(
            current
                .toString()
                .trim()
        )

        return result
    }

    private fun looksLikeCsv(
        line: String
    ): Boolean {

        if (!line.contains(',')) {
            return false
        }

        var insideQuotes = false

        for (character in line) {

            if (character == '"') {
                insideQuotes =
                    !insideQuotes
            }

            if (
                character == ',' &&
                !insideQuotes
            ) {
                return true
            }
        }

        return false
    }

    private fun looksLikeTaggedRecord(
        line: String
    ): Boolean {

        return line.startsWith("<") &&
            line.contains(">")
    }

    private fun parseTaggedRecord(
        line: String
    ): List<String> {

        val fields =
            mutableListOf<String>()

        var cursor = 0

        while (cursor < line.length) {

            val start =
                line.indexOf(
                    '<',
                    cursor
                )

            if (start < 0) {
                break
            }

            val end =
                line.indexOf(
                    '>',
                    start + 1
                )

            if (end < 0) {
                break
            }

            val tag =
                line.substring(
                    start + 1,
                    end
                ).trim()

            if (tag.isNotBlank()) {
                fields.add(tag)
            }

            cursor = end + 1
        }

        return if (fields.isEmpty()) {
            listOf(line)
        } else {
            fields
        }
    }
}
