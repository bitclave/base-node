package com.bitclave.node.repository.rtSearch

import com.bitclave.node.configuration.properties.RtSearchProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
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

    override fun getOffersIdByQuery(query: String): CompletableFuture<List<Long>> {
        return CompletableFuture.supplyAsync {
            val offerIdsResponse = restTemplate.exchange(
                    "/v1/search/?q={query}",
                    HttpMethod.GET, null,
                    object : ParameterizedTypeReference<List<Long>>() {},
                    mapOf("query" to query )
            )

            return@supplyAsync offerIdsResponse.body
        }
    }
}
