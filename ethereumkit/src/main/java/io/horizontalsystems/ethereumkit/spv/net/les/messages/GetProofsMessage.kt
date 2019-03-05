package io.horizontalsystems.ethereumkit.spv.net.les.messages

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import java.math.BigInteger

class GetProofsMessage : IMessage {

    private var requestID: Long = 0
    private var proofRequests: List<ProofRequest> = listOf()

    constructor(requestID: Long, blockHash: ByteArray, key: ByteArray, key2: ByteArray = byteArrayOf(), fromLevel: Int = 0) {
        this.requestID = requestID
        this.proofRequests = listOf(ProofRequest(blockHash, key, key2, fromLevel))
    }

    constructor(payload: ByteArray)

    override fun encoded(): ByteArray {
        val reqID = RLP.encodeBigInteger(BigInteger.valueOf(this.requestID))

        val encodedProofs = proofRequests.map { it.asRLPEncoded() }
        val proofsList = RLP.encodeList(*encodedProofs.toTypedArray())

        return RLP.encodeList(reqID, proofsList)
    }

    override fun toString(): String {
        return "GetProofs [requestID: $requestID; proofRequests: [${proofRequests.map { it.toString() }.joinToString(separator = ",")}]]"
    }

    class ProofRequest(blockHash: ByteArray, key: ByteArray, key2: ByteArray, fromLevel: Int) {

        private val blockHash: ByteArray
        private val keyHash: ByteArray
        private val key2Hash: ByteArray
        private val fromLevel: Int

        init {
            this.blockHash = blockHash
            this.keyHash = CryptoUtils.sha3(key)
            if (key2.isNotEmpty()) {
                this.key2Hash = CryptoUtils.sha3(key2)
            } else {
                this.key2Hash = key2
            }
            this.fromLevel = fromLevel
        }

        fun asRLPEncoded(): ByteArray {
            return RLP.encodeList(
                    RLP.encodeElement(blockHash),
                    RLP.encodeElement(key2Hash),
                    RLP.encodeElement(keyHash),
                    RLP.encodeInt(fromLevel)
            )
        }

        override fun toString(): String {
            return "(blockHash: ${blockHash.toHexString()}; key: ${keyHash.toHexString()}; key2: ${key2Hash.toHexString()}; fromLevel: $fromLevel)"
        }
    }
}