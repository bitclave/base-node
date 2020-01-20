package com.bitclave.node.repository.site

import com.bitclave.node.repository.entities.Site
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface SiteCrudRepository : CrudRepository<Site, Long> {

    @Transactional(readOnly = true)
    fun findByOrigin(origin: String): Site?
}
