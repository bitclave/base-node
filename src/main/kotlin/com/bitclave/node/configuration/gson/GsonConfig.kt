package com.bitclave.node.configuration.gson

import com.bitclave.node.repository.models.SignedRequest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Page
import org.springframework.http.converter.json.GsonHttpMessageConverter
import springfox.documentation.spring.web.json.Json

@Configuration
class GsonConfig {

    companion object {
        val GSON = GsonBuilder()
            .registerTypeAdapter(Page::class.java, PageResponseDeserializer())
            .registerTypeAdapter(Page::class.java, PageSerializer())
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
    fun gsonHttpMessageConverter(): GsonHttpMessageConverter {
        val converter = GsonHttpMessageConverter()
        converter.gson = GSON
        return converter
    }

    @Bean
    fun getGson(): Gson = GSON
}
