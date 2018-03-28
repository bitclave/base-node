package com.bitclave.node.repository.models

import javax.persistence.*

@Entity
data class ClientData(
        @Id val publicKey: String = "",
        @ElementCollection(fetch = FetchType.EAGER) @Column(length=10240) val data: Map<String, String> = HashMap()
) {}
