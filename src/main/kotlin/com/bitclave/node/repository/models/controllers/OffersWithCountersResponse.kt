package com.bitclave.node.repository.models.controllers

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

data class OffersWithCountersResponse(
    val counters: Map<String, Map<String, Int>>,
    private val offerIds: Page<Long>
) : PageImpl<Long>(
    offerIds.content,
    offerIds.pageable,
    offerIds.totalElements) {

    fun getPageableOfferIds(): Page<Long> {
        return PageImpl(this.content, PageRequest.of(this.number, this.size), this.totalElements)
    }
}
