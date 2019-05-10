package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.rank.OfferRankRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import com.bitclave.node.services.errors.NotFoundException

@Service
@Qualifier("v1")
class OfferRankService (
        private val offerRankRepository: RepositoryStrategy<OfferRankRepository>
) {
    fun deleteOfferRank(
            id: Long,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync {
            val deletedId = offerRankRepository.changeStrategy(strategy).deleteRankOffer(id)
            if (deletedId == 0L) {
                throw NotFoundException()
            }
            return@supplyAsync deletedId
        }
    }
}
