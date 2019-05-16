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
class OfferRankService(
    private val offerRankRepository: RepositoryStrategy<OfferRankRepository>
) {
    fun createOfferRank(
        strategy: RepositoryStrategyType,
        offerRank: OfferRank
    ): CompletableFuture<OfferRank> {
        return CompletableFuture.supplyAsync {

            val existedOfferRank: OfferRank? = offerRankRepository
                .changeStrategy(strategy)
                .findByOfferIdAndRankerId(offerRank.offerId, offerRank.rankerId)

            if (existedOfferRank != null) {
                existedOfferRank.rank = offerRank.rank
                return@supplyAsync updateOfferRank(strategy, existedOfferRank).get()
            } else {
                val readyToCreateOfferRank = OfferRank(
                    0,
                    offerRank.rank,
                    offerRank.offerId,
                    offerRank.rankerId
                )
                return@supplyAsync offerRankRepository
                    .changeStrategy(strategy)
                    .saveRankOffer(readyToCreateOfferRank)
            }
        }
    }

    fun updateOfferRank(
        strategy: RepositoryStrategyType,
        offerRank: OfferRank
    ): CompletableFuture<OfferRank> {

        return CompletableFuture.supplyAsync {
            if (offerRank.id == 0L) {
                return@supplyAsync createOfferRank(strategy, offerRank).get()
            }
            val originalOffer = offerRankRepository
                .changeStrategy(strategy)
                .findById(offerRank.id) ?: throw BadArgumentException()

            if (originalOffer.offerId != offerRank.offerId ||
                originalOffer.rankerId != offerRank.rankerId
            ) {
                throw BadArgumentException("you can change only rank in existed OfferRank")
            }

            val readyToSave = OfferRank(
                offerRank.id,
                offerRank.rank,
                offerRank.offerId,
                offerRank.rankerId,
                originalOffer.createdAt,
                Date()
            )
            return@supplyAsync offerRankRepository
                .changeStrategy(strategy)
                .saveRankOffer(readyToSave)

        }
    }

    fun getOfferRanksByOfferId(
        strategy: RepositoryStrategyType,
        offerId: Long?
    ): CompletableFuture<List<OfferRank>> {
        return CompletableFuture.supplyAsync {
            if (offerId == 0L) {
                BadArgumentException()
            }
            offerRankRepository
                .changeStrategy(strategy)
                .findByOfferId(offerId!!)
        }
    }
}
