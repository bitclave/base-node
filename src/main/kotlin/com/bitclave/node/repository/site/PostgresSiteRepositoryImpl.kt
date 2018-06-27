package com.bitclave.node.repository.site

import com.bitclave.node.repository.models.Site
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresSiteRepositoryImpl(
        val repository: SiteCrudRepository
) : SiteRepository {

    override fun saveSite(site: Site): Site {
        return repository.save(site) ?: throw DataNotSavedException()
    }

    override fun findByOrigin(origin: String): Site? {
        return repository.findByOrigin(origin)
    }

    override fun deleteByOrigin(origin: String): Long {
        val site = repository.findByOrigin(origin)
        if (site != null) {
            repository.delete(site)
        }
        return site?.id ?: 0
    }

}
