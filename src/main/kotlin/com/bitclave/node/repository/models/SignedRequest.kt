package com.bitclave.node.repository.models

data class SignedRequest<out T>(
        val data: T? = null,
        val pk: String = "",
        val sig: String = ""
) {}
