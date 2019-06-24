package com.bitclave.node.repository.rtSearch

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import java.util.concurrent.CompletableFuture

interface RtSearchRepository {

    fun getOffersIdByQuery(
        query: String,
        pageRequest: PageRequest,
        interests: List<String>? = listOf(),
        mode: String? = ""
    ): CompletableFuture<Page<Long>>

    fun getSuggestionByQuery(decodedQuery: String, size: Int): CompletableFuture<List<String>>
}
