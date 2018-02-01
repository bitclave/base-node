package com.bitclave.node.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("account")
data class AccountProperties(var salt: String = "") {}
