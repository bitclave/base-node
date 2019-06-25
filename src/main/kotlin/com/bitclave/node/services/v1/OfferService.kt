package com.bitclave.node.services.v1

import com.bitclave.node.BaseNodeApplication
import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.offer.OfferRepository
import com.bitclave.node.repository.price.OfferPriceRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.util.Date
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

@Service
@Qualifier("v1")
class OfferService(
    private val offerRepository: RepositoryStrategy<OfferRepository>,
    private val offerPriceRepository: RepositoryStrategy<OfferPriceRepository>
) {

    fun putOffer(
        id: Long,
        owner: String,
        offer: Offer,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Offer> {

        return CompletableFuture.supplyAsync(Supplier {
            var originalOffer: Offer? = null

            if (id > 0) {
                originalOffer = offerRepository.changeStrategy(strategy)
                    .findByIdAndOwner(id, owner) ?: throw BadArgumentException()
            }

            if (offer.compare.isEmpty() ||
                offer.compare.size != offer.rules.size ||
                offer.description.isEmpty() ||
                offer.title.isEmpty() ||
                offer.tags.isEmpty()
            ) {
                throw BadArgumentException()
            }

            for (item: String in offer.compare.keys) {
                if (!offer.rules.containsKey(item)) {
                    throw BadArgumentException()
                }
            }

            val createdAt = originalOffer?.createdAt ?: Date()
            val putOffer = Offer(
                id,
                owner,
                offer.offerPrices,
                offer.description,
                offer.title,
                offer.imageUrl,
                offer.worth,
                offer.tags,
                offer.compare,
                offer.rules,
                createdAt
            )
            val processedOffer = offerRepository.changeStrategy(strategy).saveOffer(putOffer)
            offerPriceRepository.changeStrategy(strategy).savePrices(processedOffer, offer.offerPrices)
            val updatedOffer = offerRepository.changeStrategy(strategy).findById(processedOffer.id)

            updatedOffer!!
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }

    fun shallowUpdateOffer(
        id: Long,
        owner: String,
        offer: Offer,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Offer> {

        return CompletableFuture.supplyAsync(Supplier {
            val originalOffer = offerRepository.changeStrategy(strategy)
                .findByIdAndOwner(id, owner) ?: throw BadArgumentException()

            if (offer.compare.isEmpty() ||
                offer.compare.size != offer.rules.size ||
                offer.description.isEmpty() ||
                offer.title.isEmpty() ||
                offer.tags.isEmpty()
            ) {
                throw BadArgumentException()
            }

            for (item: String in offer.compare.keys) {
                if (!offer.rules.containsKey(item)) {
                    throw BadArgumentException()
                }
            }

            val createdAt = originalOffer.createdAt
            val putOffer = Offer(
                id,
                owner,
                offer.offerPrices,
                offer.description,
                offer.title,
                offer.imageUrl,
                offer.worth,
                offer.tags,
                offer.compare,
                offer.rules,
                createdAt
            )
            val processedOffer = offerRepository.changeStrategy(strategy).shallowSaveOffer(putOffer)
            offerPriceRepository.changeStrategy(strategy).savePrices(processedOffer, offer.offerPrices)
            val updatedOffer = offerRepository.changeStrategy(strategy).findById(processedOffer.id)

            updatedOffer!!
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }

    fun deleteOffer(
        id: Long,
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return CompletableFuture.supplyAsync(Supplier {
            val deletedId = offerRepository.changeStrategy(strategy).deleteOffer(id, owner)
            if (deletedId == 0L) {
                throw NotFoundException()
            }
            deletedId
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }

    fun deleteOffers(
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return CompletableFuture.runAsync(Runnable {
            offerRepository.changeStrategy(strategy).deleteOffers(owner)
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }

    fun getOffers(
        id: Long,
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<Offer>> {

        return CompletableFuture.supplyAsync(Supplier {
            val repository = offerRepository.changeStrategy(strategy)

            if (id > 0 && owner != "0x0") {
                val offer = repository.findByIdAndOwner(id, owner)

                if (offer != null) {
                    return@Supplier arrayListOf(offer)
                }
                return@Supplier emptyList<Offer>()
            } else if (owner != "0x0") {
                return@Supplier repository.findByOwner(owner)
            } else {
                return@Supplier repository.findAll()
            }
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }

    fun getPageableOffers(
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Page<Offer>> {
        return CompletableFuture.supplyAsync(Supplier {
            val repository = offerRepository.changeStrategy(strategy)
            return@Supplier repository.findAll(page)
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }

    fun getPageableOffersByOwner(
        owner: String,
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Page<Offer>> {
        return CompletableFuture.supplyAsync(Supplier {
            val repository = offerRepository.changeStrategy(strategy)
            return@Supplier repository.findByOwner(owner, page)
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }

    fun getOfferTotalCount(
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return CompletableFuture.supplyAsync(Supplier {

            val repository = offerRepository.changeStrategy(strategy)

            return@Supplier repository.getTotalCount()
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }

    fun getOfferByOwnerAndTag(
        owner: String,
        tagKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<Offer>> {

        return CompletableFuture.supplyAsync(Supplier {
            val repository = offerRepository.changeStrategy(strategy)
            return@Supplier repository.getOfferByOwnerAndTag(owner, tagKey)
        }, BaseNodeApplication.FIXED_THREAD_POOL)
    }
}
