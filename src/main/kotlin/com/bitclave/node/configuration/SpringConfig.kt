package com.bitclave.node.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

open class Adapter : WebMvcConfigurer

@Configuration
class SpringConfig {

    @Bean
    fun corsConfigurer(): WebMvcConfigurer {
        return object : Adapter() {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**")
                    .allowedMethods("HEAD", "GET", "PUT", "POST", "DELETE", "PATCH")
            }
        }
    }

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}
