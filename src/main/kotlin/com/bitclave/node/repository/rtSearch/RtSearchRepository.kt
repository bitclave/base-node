package com.bitclave.node.repository.rtSearch

import java.util.concurrent.CompletableFuture

interface RtSearchRepository {

    fun getOffersIdByQuery(query: String): CompletableFuture<List<Long>>
}
