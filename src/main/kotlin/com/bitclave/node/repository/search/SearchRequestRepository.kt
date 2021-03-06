package com.bitclave.node.repository.search

import com.bitclave.node.repository.entities.SearchRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface SearchRequestRepository {

    fun save(request: SearchRequest): SearchRequest

    fun save(request: List<SearchRequest>): List<SearchRequest>

    fun deleteByIdAndOwner(id: Long, owner: String): Long

    fun deleteByOwner(owner: String): Int

    fun deleteByIdIn(ids: List<Long>): Int

    fun findById(id: Long): SearchRequest?

    fun findById(ids: List<Long>): List<SearchRequest>

    fun findByOwner(owner: String): List<SearchRequest>

    fun findByIdAndOwner(id: Long, owner: String): SearchRequest?

    fun findAll(): List<SearchRequest>

    fun findAll(pageable: Pageable): Page<SearchRequest>

    fun findAllSlice(pageable: Pageable): Slice<SearchRequest>

    fun findByOwnerInSlice(owners: List<String>, pageable: Pageable): Slice<SearchRequest>

    fun getTotalCount(): Long

    fun getRequestByOwnerAndTag(owner: String, tagKey: String): List<SearchRequest>

    fun getSearchRequestWithSameTags(): List<SearchRequest>

    fun findAllWithoutOwner(): List<SearchRequest>
}
