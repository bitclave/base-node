package com.bitclave.node.repository.models.controllers

import com.bitclave.node.repository.models.OfferSearchResultItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class OffersWithCountersResponse {

    var offerIds: List<Long> = listOf()
    val counters: Map<String, Map<String, Int>> = mapOf()
    var result: List<OfferSearchResultItem> = listOf()

    val total: Long = 0
    val numberOfElements = 0
    val first = false
    val last = false
    val number = 0
    val size = 10
    val totalPages = 0
    val totalElements: Long = 0

    fun getPageableOfferIds(): Page<Long> {
        return PageImpl(this.offerIds, PageRequest.of(this.number, this.size), this.totalElements)
    }
}
