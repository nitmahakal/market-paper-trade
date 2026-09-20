package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract

interface FnoContractProvider {

    suspend fun fetchContracts(): List<FnoContract>
}
