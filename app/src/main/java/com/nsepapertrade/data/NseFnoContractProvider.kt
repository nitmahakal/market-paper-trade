package com.nsepapertrade.data

import com.nsepapertrade.model.FnoContract

class NseFnoContractProvider : FnoContractProvider {

    override suspend fun fetchContracts(): List<FnoContract> {
        /*
         * NSE contract-master download and parsing will be added
         * in the next step.
         *
         * Keeping this provider separate allows the F&O engine
         * and repository to remain independent of the data source.
         */
        return emptyList()
    }
}
