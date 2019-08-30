package com.bitclave.node.models.services

data class CheckedExceptionResponse(
    val message: String,
    val statusText: String,
    val body: String
)
