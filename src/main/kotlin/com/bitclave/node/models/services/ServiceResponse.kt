package com.bitclave.node.models.services

import org.springframework.http.HttpHeaders

data class ServiceResponse(
    val body: Any?,
    val status: Int,
    val headers: HttpHeaders
)
