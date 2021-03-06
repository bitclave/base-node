package com.bitclave.node.services.v1.services

import com.bitclave.node.models.services.CheckedExceptionResponse
import com.bitclave.node.models.services.HttpServiceCall
import com.bitclave.node.utils.supplyAsyncEx
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

class CallStrategyExecutorHttp(
    private val restTemplate: RestTemplate
) : CallStrategyExecutor<HttpServiceCall> {

    override fun execute(endPointUrl: String, request: HttpServiceCall): CompletableFuture<ResponseEntity<*>?> {

        return supplyAsyncEx(Supplier {
            val entity = HttpEntity<Any>(request.body, request.headers)
            val url = StringBuilder(endPointUrl).append(request.path)

            if (request.queryParams.isNotEmpty()) {
                url.append("?")
                    .append(request.queryParams.entries.joinToString("&"))
            }

            try {
                return@Supplier restTemplate.exchange(
                    url.toString(),
                    request.httpMethod, entity,
                    Any::class.java,
                    request.queryParams
                )
            } catch (e: Throwable) {
                if (e is HttpStatusCodeException) {
                    return@Supplier ResponseEntity(
                        CheckedExceptionResponse(e.message ?: "", e.statusText ?: "", e.responseBodyAsString ?: ""),
                        e.responseHeaders,
                        e.statusCode
                    )
                }

                throw e
            }
        })
    }
}
