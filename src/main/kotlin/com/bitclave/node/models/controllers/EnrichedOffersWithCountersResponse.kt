package com.bitclave.node.models.controllers

import com.bitclave.node.models.OfferSearchResultItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl

data class EnrichedOffersWithCountersResponse(
    private val offers: Page<OfferSearchResultItem>,
    val counters: Map<String, Map<String, Int>>
) : PageImpl<OfferSearchResultItem>(offers.content, offers.pageable, offers.totalElements)
