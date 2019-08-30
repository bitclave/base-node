package com.bitclave.node.models

data class RequestDataTree(
    val id: Long,
    val from: String,
    val to: String,
    val root: String,
    val next: List<RequestDataTree>
)
