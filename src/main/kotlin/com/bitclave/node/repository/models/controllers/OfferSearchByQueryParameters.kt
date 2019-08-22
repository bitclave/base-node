package com.bitclave.node.repository.models.controllers

data class OfferSearchByQueryParameters(
    val searchRequestId: Long,
    val filters: Map<String, List<String>>
)
