package io.horizontalsystems.erc20kit.models

import io.horizontalsystems.ethereumkit.core.toEIP55Address
import io.horizontalsystems.ethereumkit.core.toHexString

class TransactionInfo(transaction: Transaction) {

    val transactionHash: String
    val transactionIndex: Int?
    val interTransactionIndex: Int
    val from: String
    val to: String
    val value: String
    val timestamp: Long

    var logIndex: Int? = null
    var blockHash: String? = null
    var blockNumber: Long? = null

    init {
        transactionHash = transaction.transactionHash.toHexString()
        transactionIndex = transaction.transactionIndex
        interTransactionIndex = transaction.interTransactionIndex
        logIndex = transaction.logIndex
        from = transaction.from.toEIP55Address()
        to = transaction.to.toEIP55Address()
        value = transaction.value.toString(10)
        timestamp = transaction.timestamp
        blockHash = transaction.blockHash?.toHexString()
        blockNumber = transaction.blockNumber
    }

}
