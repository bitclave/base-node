package com.bitclave.node.models.controllers

data class OfferSearchByQueryParameters(
    val searchRequestId: Long,
    val filters: Map<String, List<String>>
)
