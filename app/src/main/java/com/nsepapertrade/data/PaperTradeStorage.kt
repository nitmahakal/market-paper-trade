package com.nsepapertrade.data

import android.content.Context
import android.content.SharedPreferences

class PaperTradeStorage(
    context: Context
) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(
            "paper_trade_storage",
            Context.MODE_PRIVATE
        )

    fun getDouble(
        key: String,
        defaultValue: Double
    ): Double {
        return preferences.getString(key, null)
            ?.toDoubleOrNull()
            ?: defaultValue
    }

    fun putDouble(
        key: String,
        value: Double
    ) {
        preferences.edit()
            .putString(key, value.toString())
            .apply()
    }

    fun getString(
        key: String,
        defaultValue: String = ""
    ): String {
        return preferences.getString(key, defaultValue)
            ?: defaultValue
    }

    fun putString(
        key: String,
        value: String
    ) {
        preferences.edit()
            .putString(key, value)
            .apply()
    }

    fun remove(key: String) {
        preferences.edit()
            .remove(key)
            .apply()
    }
}
