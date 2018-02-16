package com.bitclave.node.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("ethereum")
data class EthereumProperties(var nodeUrl: String = "") {}
