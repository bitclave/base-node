package com.bitclave.node.repository.entities

import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id

@Entity
data class ClientData(
    @Id val publicKey: String = "",
    @ElementCollection(fetch = FetchType.EAGER) @Column(length = 10240) val data: Map<String, String> = HashMap()
)
