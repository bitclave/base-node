package com.bitclave.node.repository.rtSearch

import com.bitclave.node.configuration.properties.RtSearchProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.converter.json.GsonHttpMessageConverter
import org.springframework.stereotype.Repository
import org.springframework.web.client.RestTemplate
import java.util.concurrent.CompletableFuture

@Repository
class RtSearchRepositoryImpl(
    rtSearchProperties: RtSearchProperties,
    converter: GsonHttpMessageConverter
) : RtSearchRepository {

    private val restTemplate: RestTemplate = RestTemplateBuilder()
        .messageConverters(converter)
        .rootUri(rtSearchProperties.url).build()

    override fun getOffersIdByQuery(
        query: String,
        pageRequest: PageRequest,
        interests: List<String>?,
        mode: String?
    ): CompletableFuture<Page<Long>> {
        return CompletableFuture.supplyAsync {

            val parameters = mapOf(
                "query" to query,
                "page" to pageRequest.pageNumber,
                "size" to pageRequest.pageSize,
                "mode" to mode
            )
            val httpEntity = HttpEntity<List<String>>(interests)
            val offerIdsResponse = restTemplate.exchange(
                "/v1/search/?q={query}&page={page}&size={size}&mode={mode}",
                HttpMethod.POST, httpEntity,
                object : ParameterizedTypeReference<Page<Long>>() {},
                parameters
            )
            return@supplyAsync offerIdsResponse.body
        }
    }
}
