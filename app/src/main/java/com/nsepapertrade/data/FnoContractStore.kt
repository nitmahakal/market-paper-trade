package com.nsepapertrade.data

import android.content.Context
import com.nsepapertrade.model.FnoContract
import org.json.JSONArray
import org.json.JSONObject

class FnoContractStore(
    context: Context
) {

    companion object {
        private const val PREFS_NAME = "fno_contract_store"
        private const val CONTRACTS_KEY = "contracts"
        private const val LAST_UPDATE_KEY = "last_update"
    }

    private val preferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveContracts(contracts: List<FnoContract>) {
        val array = JSONArray()

        contracts.forEach { contract ->
            val item = JSONObject()
            item.put("underlying", contract.underlying)
            item.put("underlyingType", contract.underlyingType.name)
            item.put("contractType", contract.contractType.name)
            item.put("expiry", contract.expiry)
            item.put("strikePrice", contract.strikePrice)
            item.put("optionType", contract.optionType.name)
            item.put("lotSize", contract.lotSize)
            array.put(item)
        }

        preferences.edit()
            .putString(CONTRACTS_KEY, array.toString())
            .putLong(LAST_UPDATE_KEY, System.currentTimeMillis())
            .apply()
    }

    fun loadContracts(): List<FnoContract> {
        val raw = preferences.getString(CONTRACTS_KEY, null)
            ?: return emptyList()

        return try {
            val array = JSONArray(raw)
            val contracts = mutableListOf<FnoContract>()

            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)

                contracts.add(
                    FnoContract(
                        underlying = item.getString("underlying"),
                        underlyingType =
                            com.nsepapertrade.model.FnoUnderlyingType.valueOf(
                                item.getString("underlyingType")
                            ),
                        contractType =
                            com.nsepapertrade.model.FnoContractType.valueOf(
                                item.getString("contractType")
                            ),
                        expiry = item.getString("expiry"),
                        strikePrice = item.optDouble("strikePrice", 0.0),
                        optionType =
                            com.nsepapertrade.model.OptionType.valueOf(
                                item.getString("optionType")
                            ),
                        lotSize = item.getInt("lotSize")
                    )
                )
            }

            contracts
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getLastUpdateTime(): Long =
        preferences.getLong(LAST_UPDATE_KEY, 0L)
}
