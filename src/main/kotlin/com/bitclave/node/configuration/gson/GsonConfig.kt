package com.bitclave.node.configuration.gson

import com.bitclave.node.models.SignedRequest
import com.bitclave.node.models.controllers.EnrichedOffersWithCountersResponse
import com.bitclave.node.models.controllers.OffersWithCountersResponse
import com.bitclave.node.models.services.ServiceCall
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Page
import org.springframework.data.domain.Slice
import org.springframework.http.converter.json.GsonHttpMessageConverter
import springfox.documentation.spring.web.json.Json

@Configuration
class GsonConfig {

    companion object {
        val GSON = GsonBuilder()
            .registerTypeAdapter(ServiceCall::class.java, ServiceCallDeserializer())
            .registerTypeAdapter(Page::class.java, PageResponseDeserializer())
            .registerTypeAdapter(Page::class.java, PageSerializer())
            .registerTypeAdapter(OffersWithCountersResponse::class.java, PageWithCountersResponseDeserializer())
            .registerTypeAdapter(EnrichedOffersWithCountersResponse::class.java, PageWithCountersSerializer())
            .registerTypeAdapter(Slice::class.java, SliceSerializer())
            .registerTypeAdapter(SignedRequest::class.java, SignedRequestDeserializer())
            .addSerializationExclusionStrategy(AnnotationExcludeStrategy())
            .addDeserializationExclusionStrategy(SuperclassExclusionStrategy())
            .addSerializationExclusionStrategy(SuperclassExclusionStrategy())
            .registerTypeAdapter(Json::class.java, SpringfoxJsonSerializer())
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .serializeNulls()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()!!
    }

    @Bean
    @ConditionalOnMissingBean
    fun gsonMessageConverter(): GsonMessageConverter {
        return GsonMessageConverter(GSON)
    }

    @Bean
    @ConditionalOnMissingBean
    fun gsonHttpMessageConverter(): GsonHttpMessageConverter {
        val converter = GsonHttpMessageConverter()
        converter.gson = GSON

        return converter
    }

    @Bean
    @ConditionalOnMissingBean
    fun getGson(): Gson = GSON
}
