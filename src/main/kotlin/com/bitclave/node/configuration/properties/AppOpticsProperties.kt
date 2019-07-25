package com.bitclave.node.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("appoptics")
data class AppOpticsProperties (
    var serviceKey: String = ""
)
