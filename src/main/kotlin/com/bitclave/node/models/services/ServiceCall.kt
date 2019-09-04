package com.bitclave.node.models.services

enum class ServiceCallType {
    HTTP,
    RPC
}

interface ServiceCall {
    val serviceId: String
    val type: ServiceCallType
}
