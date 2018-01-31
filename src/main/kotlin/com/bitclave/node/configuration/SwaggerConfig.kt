package com.bitclave.node.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2

@Configuration
@EnableSwagger2
class SwaggerConfig {

    @Bean
    fun restApi(): Docket {
        return Docket(DocumentationType.SWAGGER_2).select().apis(
                RequestHandlerSelectors.basePackage("com.bitclave.node.controllers")).paths(
                PathSelectors.any()).build().apiInfo(apiInfo())
    }

    private fun apiInfo(): ApiInfo {
        return ApiInfo("BASE node", "Bitclave BASE platform node REST API", "0.0.1",
                "Terms of service",
                Contact("Bitclave team", "https://github.com/bitclave/base-node/issues",
                        "https://github.com/bitclave/base-node/issues"), "MIT", "license url",
                emptyList())
    }

}
