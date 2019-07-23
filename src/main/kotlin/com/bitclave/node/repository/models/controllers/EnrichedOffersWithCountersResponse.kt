package com.bitclave.node.repository.models.controllers

import com.bitclave.node.repository.models.OfferSearchResultItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest

class EnrichedOffersWithCountersResponse (offers: Page<OfferSearchResultItem>, counters: Map<String, Map<String, Int>>) {

    var counters: Map<String, Map<String, Int>> = mapOf()
    var content: List<OfferSearchResultItem> = listOf()

    var total: Long = 0
    var numberOfElements = 0
    var first = false
    var last = false
    var number = 0
    var size = 0
    var totalPages: Long = 0
    var totalElements: Long = 0
    var pageable = PageRequest(0,10)

    init {
        this.counters = counters
        this.content = offers.content

        this.totalElements = if (offers.totalElements < 10000) offers.totalElements else 10000
        this.totalPages = if (offers.size != 0) totalElements / offers.size else 0

        this.total = offers.totalElements
        this.numberOfElements = offers.numberOfElements
        this.first = offers.isFirst
        this.last = offers.isLast
        this.number = offers.number
        this.size = offers.size

        this.pageable = PageRequest(this.number, this.size)
    }
}
