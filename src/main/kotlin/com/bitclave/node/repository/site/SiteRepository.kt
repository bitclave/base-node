package com.bitclave.node.repository.site

import com.bitclave.node.repository.entities.Site

interface SiteRepository {

    fun saveSite(site: Site): Site

    fun findByOrigin(origin: String): Site?

    fun deleteByOrigin(origin: String): Long
}
