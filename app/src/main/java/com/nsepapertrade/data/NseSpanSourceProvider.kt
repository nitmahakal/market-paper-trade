package com.nsepapertrade.data

interface NseSpanSourceProvider {

    fun getSources(): List<NseSpanSource>
}
