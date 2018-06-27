package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.repository.request.RequestDataRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.utils.KeyPairUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
@Qualifier("v1")
class RequestDataService(private val requestDataRepository: RepositoryStrategy<RequestDataRepository>) {

    fun getRequestByStatus(
            fromPk: String?,
            toPk: String?,
            strategy: RepositoryStrategyType
    ): CompletableFuture<List<RequestData>> {

        return CompletableFuture.supplyAsync({
            val result: List<RequestData> =
                    if (fromPk == null && toPk != null) {
                        requestDataRepository.changeStrategy(strategy)
                                .getByTo(toPk)

                    } else if (fromPk != null && toPk == null) {
                        requestDataRepository.changeStrategy(strategy)
                                .getByFrom(fromPk)

                    } else if (fromPk != null && toPk != null) {
                        val result = requestDataRepository.changeStrategy(strategy)
                                .getByFromAndTo(fromPk, toPk)
                        if (result == null)
                            Collections.emptyList<RequestData>()
                        else
                            arrayListOf(result)

                    } else {
                        throw BadArgumentException()
                    }

            result
        })
    }

    fun request(clientPk: String, data: RequestData, strategy: RepositoryStrategyType): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync({
            val record = requestDataRepository.changeStrategy(strategy)
                    .getByFromAndTo(clientPk, data.toPk.toLowerCase())

            val request = RequestData(
                    record?.id ?: 0L,
                    clientPk,
                    data.toPk.toLowerCase(),
                    data.requestData,
                    record?.responseData ?: ""
            )
            requestDataRepository.changeStrategy(strategy)
                    .updateData(request).id
        })
    }

    fun grantAccess(
            clientId: String,
            data: RequestData,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return CompletableFuture.supplyAsync({
            if (data.responseData.isEmpty() ||
                    data.toPk != clientId ||
                    !KeyPairUtils.isValidPublicKey(data.fromPk)) {
                throw BadArgumentException()
            }

            val record = requestDataRepository.changeStrategy(strategy)
                    .getByFromAndTo(data.fromPk.toLowerCase(), clientId)

            val request = RequestData(
                    record?.id ?: 0L,
                    data.fromPk.toLowerCase(),
                    clientId,
                    record?.requestData ?: "",
                    data.responseData
            )

            requestDataRepository.changeStrategy(strategy)
                    .updateData(request).id
        })
    }

    fun deleteRequestsAndResponses(
            publicKey: String,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return CompletableFuture.runAsync({
            requestDataRepository.changeStrategy(strategy)
                    .deleteByFromAndTo(publicKey)
        })
    }

}
