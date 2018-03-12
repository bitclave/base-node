package com.bitclave.node.services

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.OfferShareData
import com.bitclave.node.repository.offer.OfferRepository
import com.bitclave.node.repository.share.OfferShareRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.DuplicateException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture

@Service
class OfferShareService(
        private val offerShareRepository: RepositoryStrategy<OfferShareRepository>,
        private val offerRepository: RepositoryStrategy<OfferRepository>
) {
    fun getShareData(
            offerOwner: String,
            accepted: Boolean?,
            strategy: RepositoryStrategyType
    ): CompletableFuture<List<OfferShareData>> {

        return CompletableFuture.supplyAsync({
            val repository = offerShareRepository.changeStrategy(strategy)
            if (accepted != null) {
                return@supplyAsync repository.findByOwnerAndAccepted(offerOwner, accepted)
            } else {
                return@supplyAsync repository.findByOwner(offerOwner)
            }
        })
    }

    fun share(clientId: String, data: OfferShareData, strategy: RepositoryStrategyType): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            if (data.clientId != clientId ||
                    data.clientResponse.isEmpty() ||
                    data.offerId <= 0) {
                throw BadArgumentException()
            }
            val existed = offerShareRepository.changeStrategy(strategy)
                    .findByOfferIdAndClientId(data.offerId, clientId)
            if (existed != null) {
                throw DuplicateException()
            }

            offerRepository.changeStrategy(strategy).findById(data.offerId)
                    ?: throw BadArgumentException()

            val shareData = OfferShareData(
                    data.offerId,
                    clientId,
                    data.offerOwner,
                    data.clientResponse,
                    BigDecimal.ZERO,
                    false
            )
            offerShareRepository.changeStrategy(strategy)
                    .saveShareData(shareData)
        })
    }

    fun acceptShareData(
            ownerId: String,
            offerId: Long,
            clientId: String,
            worth: BigDecimal,
            strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            val originShareData = offerShareRepository.changeStrategy(strategy)
                    .findByOfferIdAndClientId(offerId, clientId)

            if (originShareData == null || originShareData.offerOwner != ownerId) {
                throw BadArgumentException()
            }
            val shareData = OfferShareData(
                    originShareData.offerId,
                    originShareData.clientId,
                    originShareData.offerOwner,
                    originShareData.clientResponse,
                    worth,
                    true
            )

            offerShareRepository.changeStrategy(strategy)
                    .saveShareData(shareData)
        })
    }

}
