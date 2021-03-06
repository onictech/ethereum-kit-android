package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal

interface IAdapter {

    val name: String
    val coin: String

    val lastBlockHeight: Long?
    val syncState: EthereumKit.SyncState
    val balance: BigDecimal

    val receiveAddress: String

    val lastBlockHeightFlowable: Flowable<Unit>
    val syncStateFlowable: Flowable<Unit>
    val balanceFlowable: Flowable<Unit>
    val transactionsFlowable: Flowable<Unit>

    fun validateAddress(address: String)
    fun send(address: String, amount: BigDecimal): Single<Unit>
    fun transactions(from: Pair<String, Int>? = null, limit: Int? = null): Single<List<TransactionRecord>>
}
