package com.bitclave.node.services

import com.bitclave.node.repository.models.RequestData
import com.bitclave.node.repository.request.RequestDataRepository
import com.bitclave.node.services.errors.BadArgumentException
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class RequestDataService(private val requestDataRepository: RequestDataRepository) {

    fun getRequestByStatus(fromPk: String?, toPk: String?,
                           state: RequestData.RequestDataState): CompletableFuture<List<RequestData>> {
        return CompletableFuture.supplyAsync({
            val result: List<RequestData> =
                    if (fromPk == null && toPk != null) {
                        requestDataRepository.getByTo(toPk, state)

                    } else if (fromPk != null && toPk == null) {
                        requestDataRepository.getByFrom(fromPk, state)

                    } else if (fromPk != null && toPk != null) {
                        requestDataRepository.getByFromAndTo(fromPk, toPk, state)

                    } else {
                        throw BadArgumentException()
                    }

            result;
        })
    }

    fun request(clientPk: String, data: RequestData): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync({
            val request = RequestData(
                    -1,
                    clientPk,
                    data.toPk,
                    data.requestData,
                    "",
                    RequestData.RequestDataState.AWAIT
            )
            requestDataRepository.updateData(request).id
        })
    }

    fun response(id: Long, publicKey: String,
                 data: String?): CompletableFuture<RequestData.RequestDataState> {
        return CompletableFuture.supplyAsync({
            val original = requestDataRepository.findById(id)

            if (original == null || original.toPk != publicKey) {
                throw BadArgumentException()
            }

            val state = when {
                data.isNullOrEmpty() -> RequestData.RequestDataState.REJECT
                else -> RequestData.RequestDataState.ACCEPT
            }

            val result = RequestData(
                    original.id,
                    original.fromPk,
                    original.toPk,
                    original.requestData,
                    data ?: "",
                    state
            )

            requestDataRepository.updateData(result)

            state
        })
    }

}
