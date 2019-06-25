package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.Site
import com.bitclave.node.repository.site.SiteRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import com.bitclave.node.utils.supplyAsyncEx
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

@Service
@Qualifier("v1")
class SiteService(
    private val repository: RepositoryStrategy<SiteRepository>
) {

    private val prefix = Regex("^(http|https)", RegexOption.IGNORE_CASE)

    fun saveSiteInformation(site: Site, strategy: RepositoryStrategyType): CompletableFuture<Long> {
        return supplyAsyncEx(Supplier {
            if (site.origin.isEmpty() || site.origin.contains(prefix)) {
                throw BadArgumentException()
            } else {
                val model = Site(0, site.origin.toLowerCase(), site.publicKey, site.confidential)
                repository.changeStrategy(strategy).deleteByOrigin(model.origin)

                repository.changeStrategy(strategy).saveSite(model).id
            }
        })
    }

    fun getSite(origin: String, strategy: RepositoryStrategyType): CompletableFuture<Site> {
        return supplyAsyncEx(Supplier {
            repository.changeStrategy(strategy)
                .findByOrigin(origin.toLowerCase())
                ?: throw NotFoundException()
        })
    }
}
