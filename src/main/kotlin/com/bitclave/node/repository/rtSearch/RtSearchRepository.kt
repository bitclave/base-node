package com.bitclave.node.repository.rtSearch

import com.bitclave.node.repository.models.controllers.OffersWithCountersResponse
import org.springframework.data.domain.PageRequest
import java.util.concurrent.CompletableFuture

interface RtSearchRepository {

    fun getOffersIdByQuery(
        query: String,
        pageRequest: PageRequest,
        filters: Map<String, List<String>>? = mapOf(),
        mode: String? = ""
    ): CompletableFuture<OffersWithCountersResponse>

    fun getSuggestionByQuery(decodedQuery: String, size: Int): CompletableFuture<List<String>>
}
