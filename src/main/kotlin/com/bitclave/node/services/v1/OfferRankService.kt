package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.OfferRank
import com.bitclave.node.repository.rank.OfferRankRepository
import com.bitclave.node.services.errors.BadArgumentException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.Date

@Service
@Qualifier("v1")
class OfferRankService (
        private val offerRankRepository: RepositoryStrategy<OfferRankRepository>
) {
    fun createOfferRank(
            strategy: RepositoryStrategyType,
            offerRank: OfferRank
    ): CompletableFuture<OfferRank> {
        val readyToCreateOfferRank = OfferRank(
                0,
                offerRank.rank,
                offerRank.offerId,
                offerRank.rankerId
        )
        return CompletableFuture.supplyAsync {
            val saved = offerRankRepository
                    .changeStrategy(strategy)
                    .saveRankOffer(readyToCreateOfferRank)
            return@supplyAsync saved

        }
    }
    fun updateOfferRank(
            strategy: RepositoryStrategyType,
            offerRank: OfferRank
    ): CompletableFuture<OfferRank> {
        if (offerRank.id == 0L) {
            return createOfferRank(strategy, offerRank)
        }
        val originalOffer = offerRankRepository
                .changeStrategy(strategy)
                .findById(offerRank.id) ?: throw BadArgumentException()
        val readyToSave = OfferRank(
                offerRank.id,
                offerRank.rank,
                offerRank.offerId,
                offerRank.rankerId,
                originalOffer.createdAt,
                Date()
        )
        return CompletableFuture.supplyAsync {
            return@supplyAsync offerRankRepository
                    .changeStrategy(strategy)
                    .saveRankOffer(readyToSave)
        }
    }
}
