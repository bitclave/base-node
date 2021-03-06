package com.bitclave.node.repository.rtSearch

import com.bitclave.node.configuration.properties.RtSearchProperties
import com.bitclave.node.models.controllers.OffersWithCountersResponse
import com.bitclave.node.utils.supplyAsyncEx
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.converter.json.GsonHttpMessageConverter
import org.springframework.stereotype.Repository
import org.springframework.web.client.RestTemplate
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

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
        filters: Map<String, List<String>>?,
        mode: String?
    ): CompletableFuture<OffersWithCountersResponse> {
        return supplyAsyncEx(Supplier {

            val parameters = mapOf(
                    "query" to query,
                    "page" to pageRequest.pageNumber,
                    "size" to pageRequest.pageSize,
                    "mode" to mode
            )
            val httpEntity = HttpEntity(filters!!)
            val offerIdsResponse = restTemplate.exchange(
                    "/v1/search/?q={query}&page={page}&size={size}&mode={mode}",
                    HttpMethod.POST, httpEntity,
                    object : ParameterizedTypeReference<OffersWithCountersResponse>() {},
                    parameters
            )

            offerIdsResponse.body
        })
    }

    override fun getSuggestionByQuery(decodedQuery: String, size: Int): CompletableFuture<List<String>> {
        return supplyAsyncEx(Supplier {

            val parameters = mapOf(
                    "query" to decodedQuery,
                    "size" to size
            )
            val offerIdsResponse = restTemplate.exchange(
                    "/v1/suggest?q={query}&s={size}",
                    HttpMethod.GET, null,
                    object : ParameterizedTypeReference<List<String>>() {},
                    parameters
            )

            offerIdsResponse.body
        })
    }
}
