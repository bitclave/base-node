package com.bitclave.node.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("rtsearch")
data class RtSearchProperties(
    var url: String = ""
)
