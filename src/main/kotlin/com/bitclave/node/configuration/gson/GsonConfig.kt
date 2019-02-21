package com.bitclave.node.configuration.gson

import com.google.gson.GsonBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.GsonHttpMessageConverter


@Configuration
class WebConfigurer {

    @Bean
    fun gsonHttpMessageConverter(): GsonHttpMessageConverter {
        val converter = GsonHttpMessageConverter()
        converter.gson = GsonBuilder()
                .addSerializationExclusionStrategy(AnnotationExcludeStrategy())
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .disableHtmlEscaping()
                .create()
        return converter
    }
}
