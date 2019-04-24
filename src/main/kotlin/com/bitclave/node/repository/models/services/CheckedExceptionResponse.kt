package com.bitclave.node.repository.models.services

data class CheckedExceptionResponse(
    val message: String,
    val statusText: String,
    val body: String
)
