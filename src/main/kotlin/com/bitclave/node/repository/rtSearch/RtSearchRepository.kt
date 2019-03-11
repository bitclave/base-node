package com.bitclave.node.repository.rtSearch

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import java.util.concurrent.CompletableFuture

interface RtSearchRepository {

    fun getOffersIdByQuery(query: String, pageRequest: PageRequest): CompletableFuture<Page<Long>>
}
