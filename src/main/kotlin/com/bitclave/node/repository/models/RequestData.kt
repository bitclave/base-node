package com.bitclave.node.repository.models

import javax.persistence.*

@Entity
data class RequestData(
        @Id @GeneratedValue(strategy = GenerationType.TABLE) val id: Long = -1,
        val fromPk: String = "",
        val toPk: String = "",
        @Column(length = 2000) val requestData: String = "",
        @Column(length = 2000) val responseData: String = "",
        val state: RequestDataState = RequestDataState.UNDEFINED
) {
    enum class RequestDataState {
        UNDEFINED, AWAIT, ACCEPT, REJECT
    }
}
