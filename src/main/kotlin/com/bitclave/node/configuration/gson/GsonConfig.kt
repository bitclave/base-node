package com.bitclave.node.configuration.gson

import com.bitclave.node.repository.models.SignedRequest
import com.google.gson.GsonBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Page
import org.springframework.http.converter.json.GsonHttpMessageConverter
import springfox.documentation.spring.web.json.Json

@Configuration
class WebConfigurer {

    @Bean
    fun gsonHttpMessageConverter(): GsonHttpMessageConverter {
        val converter = GsonHttpMessageConverter()
        converter.gson = GsonBuilder()
                .registerTypeAdapter(Page::class.java, PageSerializer())
                .registerTypeAdapter(SignedRequest::class.java, SignedRequestDeserializer())
                .addSerializationExclusionStrategy(AnnotationExcludeStrategy())
                .addDeserializationExclusionStrategy(SuperclassExclusionStrategy())
                .addSerializationExclusionStrategy(SuperclassExclusionStrategy())
                .registerTypeAdapter(Json::class.java, SpringfoxJsonSerializer())
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .serializeNulls()
//                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
        return converter
    }
}
