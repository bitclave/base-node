package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.entities.RequestData
import com.bitclave.node.models.RequestDataTree
import com.bitclave.node.repository.request.RequestDataRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.utils.KeyPairUtils
import com.bitclave.node.utils.runAsyncEx
import com.bitclave.node.utils.supplyAsyncEx
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.runAsync
import java.util.function.Supplier

@Service
@Qualifier("v1")
class RequestDataService(private val requestDataRepository: RepositoryStrategy<RequestDataRepository>) {

    fun getRequestByParams(
        strategy: RepositoryStrategyType,
        fromPk: String? = null,
        toPk: String? = null
    ): CompletableFuture<List<RequestData>> {

        return supplyAsyncEx(Supplier {
            val result: List<RequestData> =
                if (fromPk == null && toPk != null) {
                    requestDataRepository.changeStrategy(strategy)
                        .getByTo(toPk)
                } else if (fromPk != null && toPk == null) {
                    requestDataRepository.changeStrategy(strategy)
                        .getByFrom(fromPk)
                } else if (fromPk != null && toPk != null) {
                    requestDataRepository.changeStrategy(strategy)
                        .getByFromAndTo(fromPk, toPk)
                } else {
                    throw BadArgumentException()
                }

            result
        })
    }

    fun request(clientPk: String, data: List<RequestData>, strategy: RepositoryStrategyType): CompletableFuture<Void> {
        return runAsync(Runnable {
            this.checkRequestData(data)
            val toPk = data[0].toPk.toLowerCase()

            val existed = requestDataRepository.changeStrategy(strategy)
                .getByFromAndTo(clientPk, toPk)

            val result: List<RequestData> = data
                .filter { item -> existed.find { existedItem -> existedItem.requestData == item.requestData } == null }
                .map { item ->
                    item.copy(
                        fromPk = clientPk,
                        toPk = toPk,
                        rootPk = toPk,
                        responseData = ""
                    )
                }

            requestDataRepository.changeStrategy(strategy)
                .saveAll(result)
        })
    }

    fun grantAccess(
        clientId: String,
        data: List<RequestData>,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return runAsync(Runnable {
            this.checkRequestData(data)

            data.forEach {
                if (it.responseData.isEmpty() ||
                    it.requestData.isEmpty() ||
                    it.toPk != clientId ||
                    !KeyPairUtils.isValidPublicKey(it.rootPk) ||
                    !KeyPairUtils.isValidPublicKey(it.fromPk)
                ) {
                    throw BadArgumentException()
                }
            }

            val fromPk = data[0].fromPk.toLowerCase()

            val existed = requestDataRepository.changeStrategy(strategy)
                .getByFromAndTo(fromPk, clientId)

            val deprecatedItems = existed.filter { it.rootPk.isEmpty() }

            if (deprecatedItems.isNotEmpty()) {
                requestDataRepository.changeStrategy(strategy).deleteByIds(deprecatedItems.map { it.id })
            }

            val result = data.map { item ->
                if (item.rootPk != item.toPk) {
                    val isAccepted = acceptedDataForPk(item.rootPk, item.toPk, item.rootPk, item.requestData, strategy)
                    if (!isAccepted) {
                        throw BadArgumentException("data not available for share")
                    }
                }

                val foundItem = existed.find { item.requestData == it.requestData }

                return@map foundItem?.copy(responseData = item.responseData)
                    ?: item.copy(
                        fromPk = fromPk,
                        toPk = clientId,
                        rootPk = item.rootPk,
                        requestData = item.requestData,
                        responseData = item.responseData
                    )
            }

            requestDataRepository.changeStrategy(strategy)
                .saveAll(result)
        })
    }

    fun revokeAccess(
        clientId: String,
        data: List<RequestData>,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return CompletableFuture.runAsync {
            this.checkRequestData(data)

            data.forEach {
                if (it.requestData.isEmpty() ||
                    it.toPk != clientId ||
                    !KeyPairUtils.isValidPublicKey(it.fromPk)
                ) {
                    throw BadArgumentException()
                }
            }

            val idsForDelete: MutableList<Long> = mutableListOf()

            data.forEach { request ->
                val tree = findDependencies(request.toPk, request.rootPk, request.requestData, strategy)
                    .filter { it.from == request.fromPk }

                val list = treeToListOfRequests(tree)

                idsForDelete.addAll(list.map { itemResult -> itemResult.id })
            }

            requestDataRepository
                .changeStrategy(strategy)
                .deleteByIds(idsForDelete)
        }
    }

    fun deleteRequestsAndResponses(
        publicKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return runAsyncEx(Runnable {
            requestDataRepository.changeStrategy(strategy).deleteByFromAndTo(publicKey)
        })
    }

    private fun acceptedDataForPk(
        to: String,
        from: String,
        root: String,
        requestData: String,
        strategy: RepositoryStrategyType
    ): Boolean {
        val tree = findDependencies(to, root, requestData, strategy)

        return searchInTreeOfRequests(root, from, tree)
    }

    private fun searchInTreeOfRequests(root: String, from: String, tree: List<RequestDataTree>): Boolean {
        tree.forEach {
            if (it.from == from && (it.to == root || it.root == root)) {
                return true
            } else {
                if (searchInTreeOfRequests(root, from, it.next)) {
                    return true
                }
            }
        }

        return false
    }

    private fun treeToListOfRequests(tree: List<RequestDataTree>): List<RequestDataTree> {
        val result = mutableListOf<RequestDataTree>()
        result.addAll(tree)
        tree.forEach {
            result.addAll(treeToListOfRequests(it.next))
        }

        return result
    }

    private fun findDependencies(
        to: String,
        root: String,
        requestData: String,
        strategy: RepositoryStrategyType
    ): List<RequestDataTree> {
        val allItems = requestDataRepository
            .changeStrategy(strategy)
            .getByRequestDataAndRootPk(requestData, root)

        return this.makeTreeOfRequests(allItems, requestData, to, root)
    }

    private fun makeTreeOfRequests(
        items: List<RequestData>,
        requestData: String,
        to: String,
        root: String
    ): List<RequestDataTree> {
        val filtered = items.filter { it.toPk == to && it.rootPk == root && it.requestData == requestData }
        val result = mutableListOf<RequestDataTree>()

        if (filtered.isNotEmpty()) {
            val nextItems = items.toList().minus(filtered)

            filtered.forEach {
                val rtResult = makeTreeOfRequests(nextItems, requestData, it.fromPk, root)
                val rt = RequestDataTree(it.id, it.fromPk, it.toPk, it.rootPk, rtResult)
                result.add(rt)
            }
        }

        return result
    }

    private fun checkRequestData(data: List<RequestData>) {
        if (data.isEmpty()) {
            throw BadArgumentException("list of Data not must be empty")
        }

        val fromPk = data[0].fromPk
        val toPk = data[0].toPk
        data.forEach {
            if (it.fromPk != fromPk || it.toPk != toPk) {
                throw BadArgumentException("all request data should be has same fromPk and toPk")
            }
            if (it.requestData.isEmpty()) {
                throw BadArgumentException("requestData should be not empty")
            }
        }
    }
}
