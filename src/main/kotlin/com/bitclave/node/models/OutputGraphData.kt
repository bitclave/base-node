package com.bitclave.node.models

import com.bitclave.node.repository.entities.RequestData

enum class LinkType {
    SHARE,
    RESHARE
}

data class GraphLink(
    val from: Int,
    val to: Int,
    val key: String,
    val type: LinkType
)

data class OutputGraphData(
    val clients: Set<String>,
    val links: MutableList<GraphLink> = mutableListOf()
) {

    fun addLink(request: RequestData) {
        val type = if (request.rootPk.isBlank() || request.rootPk == request.toPk) LinkType.SHARE else LinkType.RESHARE

        // here we use for "from" -> toPk and "to" -> fromPk. because we get data from requestData model.
        // (where fromPk is publicKey of client who create request)
        links.add(GraphLink(clients.indexOf(request.toPk), clients.indexOf(request.fromPk), request.requestData, type))
    }
}
