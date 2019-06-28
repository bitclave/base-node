package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.OfferResultAction
import com.bitclave.node.repository.models.OfferInteraction
import com.bitclave.node.repository.models.OfferShareData
import com.bitclave.node.repository.offer.OfferRepository
import com.bitclave.node.repository.search.SearchRequestRepository
import com.bitclave.node.repository.search.offer.OfferSearchRepository
import com.bitclave.node.repository.search.interaction.OfferInteractionRepository
import com.bitclave.node.repository.share.OfferShareRepository
import com.bitclave.node.services.errors.AccessDeniedException
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.DuplicateException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.Date
import java.util.concurrent.CompletableFuture

@Service
@Qualifier("v1")
class OfferShareService(
    private val offerShareRepository: RepositoryStrategy<OfferShareRepository>,
    private val offerRepository: RepositoryStrategy<OfferRepository>,
    private val offerSearchRepository: RepositoryStrategy<OfferSearchRepository>,
    private val searchRequestRepository: RepositoryStrategy<SearchRequestRepository>,
    private val offerInteractionRepository: RepositoryStrategy<OfferInteractionRepository>
) {
    fun getShareData(
        offerOwner: String,
        accepted: Boolean?,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<OfferShareData>> {

        return CompletableFuture.supplyAsync {
            val repository = offerShareRepository.changeStrategy(strategy)
            if (accepted != null) {
                return@supplyAsync repository.findByOfferOwnerAndAccepted(offerOwner, accepted)
            } else {
                return@supplyAsync repository.findByOfferOwner(offerOwner)
            }
        }
    }

    fun grantAccess(
        clientId: String,
        data: OfferShareData,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return CompletableFuture.runAsync {
            val offerSearch = offerSearchRepository.changeStrategy(strategy)
                .findById(data.offerSearchId)
                ?: throw BadArgumentException("offer search id not exist")

            searchRequestRepository.changeStrategy(strategy)
                .findByIdAndOwner(offerSearch.searchRequestId, clientId)
                ?: throw BadArgumentException("searchRequestId does not exist")

            if (data.clientResponse.isEmpty()) {
                throw BadArgumentException("empty response data")
            }

            if (offerShareRepository
                    .changeStrategy(strategy)
                    .findByOfferSearchId(data.offerSearchId) != null
            ) {
                throw DuplicateException()
            }

            val offer = offerRepository
                .changeStrategy(strategy)
                .findById(offerSearch.offerId)
                ?: throw BadArgumentException("offer id not exist")

            val state = offerInteractionRepository
                .changeStrategy(strategy)
                .findByOfferIdAndOwner(offerSearch.offerId, offerSearch.owner)
                ?: OfferInteraction(0, offerSearch.owner, offerSearch.offerId)

            val price = offer.offerPrices.find { it.id == data.priceId }
                ?: throw BadArgumentException("priceId should be in offer")

            val shareData = OfferShareData(
                offerSearch.id,
                offer.owner,
                clientId,
                data.clientResponse,
                price.worth,
                false,
                data.priceId
            )

            offerShareRepository
                .changeStrategy(strategy)
                .saveShareData(shareData)

            offerInteractionRepository
                .changeStrategy(strategy)
                .save(state.copy(state = OfferResultAction.ACCEPT, updatedAt = Date()))
        }
    }

    fun acceptShareData(
        offerOwner: String,
        offerSearchId: Long,
        worth: BigDecimal,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            offerSearchRepository
                .changeStrategy(strategy)
                .findById(offerSearchId)
                ?: throw BadArgumentException("offer search id not exist")

            val originShareData = offerShareRepository
                .changeStrategy(strategy)
                .findByOfferSearchId(offerSearchId)
                ?: throw BadArgumentException("share data id not exist")

            if (offerOwner != originShareData.offerOwner) {
                throw AccessDeniedException()
            }

            if (BigDecimal(originShareData.worth).compareTo(worth) != 0) {
                throw BadArgumentException("incorrect worth value")
            }

            val shareData = OfferShareData(
                originShareData.offerSearchId,
                originShareData.offerOwner,
                originShareData.clientId,
                originShareData.clientResponse,
                originShareData.worth,
                true,
                originShareData.priceId
            )

            offerShareRepository.changeStrategy(strategy)
                .saveShareData(shareData)
        }
    }
}
