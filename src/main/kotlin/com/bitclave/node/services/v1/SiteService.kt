package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.Site
import com.bitclave.node.repository.site.SiteRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
@Qualifier("v1")
class SiteService(
        private val repository: RepositoryStrategy<SiteRepository>
) {

    private val prefix = Regex("^(http|https)", RegexOption.IGNORE_CASE)

    fun saveSiteInformation(site: Site, strategy: RepositoryStrategyType): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync({
            if (site.origin.isEmpty() || site.origin.contains(prefix)) {
                throw BadArgumentException()

            } else {
                val model = Site(0, site.origin.toLowerCase(), site.publicKey, site.confidential)
                repository.changeStrategy(strategy).deleteByOrigin(model.origin)

                return@supplyAsync repository.changeStrategy(strategy).saveSite(model).id
            }
        })
    }

    fun getSite(origin: String, strategy: RepositoryStrategyType): CompletableFuture<Site> {
        return CompletableFuture.supplyAsync({
            val site = repository.changeStrategy(strategy).findByOrigin(origin.toLowerCase())

            return@supplyAsync site ?: throw NotFoundException()
        })
    }

}
