package com.nsepapertrade.data

data class FnoMarginUpdateResult(
    val success: Boolean,
    val marginCount: Int,
    val updateTime: Long,
    val message: String
)
