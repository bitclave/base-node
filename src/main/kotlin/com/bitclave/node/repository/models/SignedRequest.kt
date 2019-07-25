package com.bitclave.node.repository.models

data class SignedRequest<T>(
    val data: T? = null,
    val pk: String = "",
    val sig: String = "",
    val nonce: Long = 0,
    val rawData: String = ""
) {
    fun hasSignature(): Boolean = this.sig.isNotBlank()
}
