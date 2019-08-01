package com.bitclave.node.repository.models.controllers

import com.bitclave.node.repository.models.OfferSearchResultItem
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl

class EnrichedOffersWithCountersResponse(
    offers: Page<OfferSearchResultItem>,
    val counters: Map<String, Map<String, Int>>
) : PageImpl<OfferSearchResultItem>(offers.content, offers.pageable, offers.totalElements)
