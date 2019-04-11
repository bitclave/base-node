package com.bitclave.node.repository.models.services

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod

data class HttpServiceCall(
    override val serviceId: String,
    override val type: ServiceCallType,
    val httpMethod: HttpMethod,
    val path: String,
    val queryParams: Map<String, String> = emptyMap(),
    val headers: HttpHeaders = HttpHeaders(),
    val body: Any? = null
) : ServiceCall
