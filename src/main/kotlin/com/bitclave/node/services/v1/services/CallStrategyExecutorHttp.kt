package com.bitclave.node.services.v1.services

import com.bitclave.node.repository.models.services.CheckedExceptionResponse
import com.bitclave.node.repository.models.services.HttpServiceCall
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.util.concurrent.CompletableFuture

class CallStrategyExecutorHttp(
    private val restTemplate: RestTemplate
) : CallStrategyExecutor<HttpServiceCall> {

    override fun execute(endPointUrl: String, request: HttpServiceCall): CompletableFuture<ResponseEntity<*>?> {

        return CompletableFuture.supplyAsync {
            val entity = HttpEntity<Any>(request.body, request.headers)
            val url = StringBuilder(endPointUrl).append(request.path)

            if (request.queryParams.isNotEmpty()) {
                url.append("?")
                    .append(request.queryParams.entries.joinToString("&"))
            }

            try {
                return@supplyAsync restTemplate.exchange(
                    url.toString(),
                    request.httpMethod, entity,
                    Any::class.java,
                    request.queryParams
                )
            } catch (e: HttpClientErrorException) {
                return@supplyAsync ResponseEntity<CheckedExceptionResponse>(
                    CheckedExceptionResponse(e.message ?: "", e.statusText ?: "", e.responseBodyAsString ?: ""),
                    e.responseHeaders,
                    e.statusCode
                )
            }
        }
    }
}
