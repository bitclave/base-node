package com.bitclave.node.repository.request

import com.bitclave.node.repository.entities.RequestData
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface RequestDataCrudRepository : CrudRepository<RequestData, Long> {

    fun findByFromPk(from: String): List<RequestData>

    fun findByToPk(to: String): List<RequestData>

    fun findByFromPkAndToPk(from: String, to: String): List<RequestData>

    fun findByFromPkAndToPkAndRequestData(from: String, to: String, requestData: String): RequestData?

    fun findByRequestDataAndRootPk(requestData: String, rootPk: String): List<RequestData>

    @Query(
        value = """
            SELECT * FROM request_data WHERE to_pk = ?1 AND from_pk IN ?2 AND request_data IN ?3
        """,
        nativeQuery = true
    )
    fun getByFromAndToAndKeys(to: String, from: List<String>, keys: List<String>): List<RequestData>

    @Query(
        value = """
            SELECT * FROM request_data WHERE to_pk = ?1 AND from_pk IN ?2
        """,
        nativeQuery = true
    )
    fun getByFromAndTo(to: String, from: List<String>): List<RequestData>

    @Query(
        value = """
            SELECT * FROM request_data WHERE (from_pk IN ?1 OR to_pk IN ?1) AND request_data IN ?2 AND root_pk=?3
        """,
        nativeQuery = true
    )
    fun getReshareByClientsAndKeysAndRootPk(
        clientsPk: List<String>,
        keys: List<String>,
        rootPk: String
    ): List<RequestData>

    @Query(
        value = """
            SELECT * FROM request_data WHERE (from_pk IN ?1 OR to_pk IN ?1) AND root_pk=?2
        """,
        nativeQuery = true
    )
    fun getReshareByClientsAndRootPk(
        clientsPk: List<String>,
        rootPk: String
    ): List<RequestData>

    fun deleteByIdIn(ids: List<Long>)
}
