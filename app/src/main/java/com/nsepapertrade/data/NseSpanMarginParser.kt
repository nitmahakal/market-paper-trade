package com.nsepapertrade.data

interface NseSpanMarginParser {

    fun parse(
        lines: List<String>
    ): List<NseSpanMarginRecord>
}
