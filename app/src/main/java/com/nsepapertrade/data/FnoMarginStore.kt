package com.nsepapertrade.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class FnoMarginStore(
    context: Context
) {

    companion object {
        private const val PREFS_NAME =
            "fno_margin_store"

        private const val MARGINS_KEY =
            "margins"
    }

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun saveMargins(
        margins: List<FnoContractMargin>
    ) {

        val array =
            JSONArray()

        margins.forEach { margin ->

            array.put(
                JSONObject().apply {

                    put(
                        "contractSymbol",
                        margin.contractSymbol
                    )

                    put(
                        "futuresMargin",
                        margin.futuresMargin
                    )

                    put(
                        "optionSellMargin",
                        margin.optionSellMargin
                    )

                    put(
                        "timestamp",
                        margin.timestamp
                    )
                }
            )
        }

        preferences.edit()
            .putString(
                MARGINS_KEY,
                array.toString()
            )
            .apply()
    }

    fun loadMargins():
        List<FnoContractMargin> {

        val raw =
            preferences.getString(
                MARGINS_KEY,
                null
            )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(raw)

            buildList {

                for (
                    index in
                    0 until array.length()
                ) {

                    val item =
                        array.getJSONObject(index)

                    add(
                        FnoContractMargin(
                            contractSymbol =
                                item.optString(
                                    "contractSymbol"
                                ),

                            futuresMargin =
                                item.optDouble(
                                    "futuresMargin",
                                    0.0
                                ),

                            optionSellMargin =
                                item.optDouble(
                                    "optionSellMargin",
                                    0.0
                                ),

                            timestamp =
                                item.optLong(
                                    "timestamp",
                                    0L
                                )
                        )
                    )
                }
            }

        } catch (_: Exception) {

            emptyList()
        }
    }

    fun clear() {

        preferences.edit()
            .remove(MARGINS_KEY)
            .apply()
    }
}
