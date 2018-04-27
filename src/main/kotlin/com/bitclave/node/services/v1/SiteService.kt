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
                repository.changeStrategy(strategy).deleteByOrigin(site.origin)

                return@supplyAsync repository.changeStrategy(strategy).saveSite(site).id
            }
        })
    }

    fun getSite(origin: String, strategy: RepositoryStrategyType): CompletableFuture<Site> {
        return CompletableFuture.supplyAsync({
            val site = repository.changeStrategy(strategy).findByOrigin(origin)

            return@supplyAsync site ?: throw NotFoundException()
        })
    }

}
