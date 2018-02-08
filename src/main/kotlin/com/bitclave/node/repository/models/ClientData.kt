package com.bitclave.node.repository.models

import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id

@Entity
data class ClientData(
        @Id val publicKey: String = "",
        @ElementCollection(fetch = FetchType.EAGER) val data: Map<String, String> = HashMap()
) {}
