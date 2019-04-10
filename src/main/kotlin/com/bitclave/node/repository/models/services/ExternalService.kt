package com.bitclave.node.repository.models.services

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class ExternalService(
    @Column(length = 256, unique = true) @Id val publicKey: String = "",
    @Column(length = 256, unique = true) val endpoint: String = ""
)
