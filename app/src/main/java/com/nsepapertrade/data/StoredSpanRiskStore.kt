package com.nsepapertrade.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class StoredSpanRiskStore(
    context: Context
) {

    companion object {
        private const val PREFS_NAME =
            "fno_span_risk_store"

        private const val RECORDS_KEY =
            "span_risk_records"
    }

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    fun saveRecords(
        records: List<StoredSpanRiskRecord>
    ) {

        val array =
            JSONArray()

        records.forEach { record ->

            val item =
                JSONObject()

            item.put(
                "contractSymbol",
                record.contractSymbol
            )

            val riskArray =
                JSONArray()

            record.riskArray.forEach { value ->
                riskArray.put(value)
            }

            item.put(
                "riskArray",
                riskArray
            )

            item.put(
                "contractValueFactor",
                record.contractValueFactor
            )

            item.put(
                "timestamp",
                record.timestamp
            )

            array.put(item)
        }

        preferences.edit()
            .putString(
                RECORDS_KEY,
                array.toString()
            )
            .apply()
    }

    fun loadRecords():
        List<StoredSpanRiskRecord> {

        val raw =
            preferences.getString(
                RECORDS_KEY,
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

                    val riskArrayJson =
                        item.optJSONArray(
                            "riskArray"
                        )
                            ?: continue

                    val riskArray =
                        buildList {

                            for (
                                scenarioIndex in
                                0 until riskArrayJson.length()
                            ) {

                                add(
                                    riskArrayJson
                                        .optDouble(
                                            scenarioIndex,
                                            Double.NaN
                                        )
                                )
                            }
                        }

                    if (
                        riskArray.size != 16 ||
                        riskArray.any {
                            it.isNaN() ||
                                it.isInfinite()
                        }
                    ) {
                        continue
                    }

                    add(
                        StoredSpanRiskRecord(
                            contractSymbol =
                                item.optString(
                                    "contractSymbol"
                                ),

                            riskArray =
                                riskArray,

                            contractValueFactor =
                                item.optDouble(
                                    "contractValueFactor",
                                    1.0
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
            .remove(RECORDS_KEY)
            .apply()
    }
}
