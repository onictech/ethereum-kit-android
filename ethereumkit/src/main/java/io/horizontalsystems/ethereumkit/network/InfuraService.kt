package io.horizontalsystems.ethereumkit.network

import com.google.gson.GsonBuilder
import io.horizontalsystems.ethereumkit.core.EthereumKit.InfuraCredentials
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Block
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.reactivex.Single
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import java.math.BigInteger

class InfuraService(private val networkType: NetworkType, private val infuraCredentials: InfuraCredentials) {

    private val service: InfuraServiceAPI

    private val baseUrl: String
        get() {
            val subdomain = when (networkType) {
                NetworkType.Ropsten -> "ropsten"
                NetworkType.Kovan -> "kovan"
                else -> "mainnet"
            }
            return "https://$subdomain.infura.io/v3/"
        }

    init {
        val logger = HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY)

        val headersInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            infuraCredentials.secretKey?.let { secretKey ->
                requestBuilder.header("Authorization", Credentials.basic("", secretKey))
            }
            requestBuilder.header("Content-Type", "application/json")
            requestBuilder.header("Accept", "application/json")
            chain.proceed(requestBuilder.build())
        }

        val httpClient = OkHttpClient.Builder()
                .addInterceptor(logger)
                .addInterceptor(headersInterceptor)

        val gson = GsonBuilder()
                .setLenient()
                .registerTypeAdapter(BigInteger::class.java, BigIntegerTypeAdapter())
                .registerTypeAdapter(Long::class.java, LongTypeAdapter())
                .registerTypeAdapter(Int::class.java, IntTypeAdapter())
                .create()

        val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build()

        service = retrofit.create(InfuraServiceAPI::class.java)
    }

    fun getLastBlockHeight(): Single<Long> {
        val request = Request("eth_blockNumber")
        return service.makeRequestForBigInteger(infuraCredentials.projectId, request).flatMap {
            returnResultOrError(it).map { blockNumber ->
                blockNumber.toLong()
            }
        }
    }

    fun getTransactionCount(address: ByteArray): Single<Long> {
        val request = Request("eth_getTransactionCount", listOf(address.toHexString(), "latest"))
        return service.makeRequestForBigInteger(infuraCredentials.projectId, request).flatMap {
            returnResultOrError(it).map { txCount ->
                txCount.toLong()
            }
        }
    }

    fun getBalance(address: ByteArray): Single<BigInteger> {
        val request = Request("eth_getBalance", listOf(address.toHexString(), "latest"))
        return service.makeRequestForBigInteger(infuraCredentials.projectId, request).flatMap {
            returnResultOrError(it)
        }
    }

    fun send(signedTransaction: ByteArray): Single<Unit> {
        val request = Request("eth_sendRawTransaction", listOf(signedTransaction.toHexString()))
        return service.makeRequestForString(infuraCredentials.projectId, request).flatMap {
            returnResultOrError(it)
        }.map { Unit }
    }

    fun getLogs(address: ByteArray?, fromBlock: Long?, toBlock: Long?, topics: List<ByteArray?>): Single<List<EthereumLog>> {
        val fromBlockStr = fromBlock?.toBigInteger()?.toString(16)?.let { "0x$it" } ?: "earliest"
        val toBlockStr = toBlock?.toBigInteger()?.toString(16)?.let { "0x$it" } ?: "latest"
        val params: MutableMap<String, Any> = mutableMapOf(
                "fromBlock" to fromBlockStr,
                "toBlock" to toBlockStr,
                "topics" to topics.map { it?.toHexString() })

        address?.let {
            params["address"] = address.toHexString()
        }

        val request = Request("eth_getLogs", listOf(params))

        return service.makeRequestForLogs(infuraCredentials.projectId, request).flatMap {
            returnResultOrError(it)
        }
    }

    fun getStorageAt(contractAddress: ByteArray, position: String, blockNumber: Long?): Single<String> {
        val request = Request("eth_getStorageAt", listOf(contractAddress.toHexString(), position, "latest"))
        return service.makeRequestForString(infuraCredentials.projectId, request).flatMap {
            returnResultOrError(it)
        }
    }

    fun getBlockByNumber(blockNumber: Long): Single<Block> {
        val request = Request("eth_getBlockByNumber", listOf("0x${blockNumber.toString(16)}", false))
        return service.makeRequestForBlock(infuraCredentials.projectId, request).flatMap {
            returnResultOrError(it)
        }
    }

    fun call(contractAddress: ByteArray, data: ByteArray, blockNumber: Long?): Single<String> {
        val request = Request("eth_call", listOf(mapOf("to" to contractAddress.toHexString(), "data" to data.toHexString()), "latest"))
        return service.makeRequestForString(infuraCredentials.projectId, request).flatMap {
            returnResultOrError(it)
        }
    }

    private fun <T> returnResultOrError(response: Response<T>): Single<T> {
        return if (response.error != null) {
            Single.error(Exception(response.error.message))
        } else {
            Single.just(response.result)
        }
    }

    class Request(val method: String, val params: List<Any> = listOf(), val id: Long = 1, val jsonrpc: String = "2.0")
    class Error(val code: Int, val message: String)
    class Response<T>(val result: T?, val error: Error?, val id: Long, val jsonrpc: String)

    private interface InfuraServiceAPI {
        @POST("{projectId}")
        fun makeRequestForBigInteger(@Path("projectId") apiKey: String,
                                     @Body request: Request): Single<Response<BigInteger>>

        @POST("{projectId}")
        fun makeRequestForString(@Path("projectId") apiKey: String,
                                 @Body request: Request): Single<Response<String>>

        @POST("{projectId}")
        fun makeRequestForLogs(@Path("projectId") apiKey: String,
                               @Body request: Request): Single<Response<List<EthereumLog>>>

        @POST("{projectId}")
        fun makeRequestForBlock(@Path("projectId") apiKey: String,
                                @Body request: Request): Single<Response<Block>>
    }
}
