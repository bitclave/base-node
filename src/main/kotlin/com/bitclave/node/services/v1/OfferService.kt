package com.bitclave.node.services.v1

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.models.Offer
import com.bitclave.node.repository.offer.OfferRepository
import com.bitclave.node.repository.price.OfferPriceRepository
import com.bitclave.node.repository.rank.OfferRankRepository
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import com.bitclave.node.utils.runAsyncEx
import com.bitclave.node.utils.supplyAsyncEx
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import java.util.Date
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import kotlin.system.measureTimeMillis

@Service
@Qualifier("v1")
class OfferService(
    private val offerRepository: RepositoryStrategy<OfferRepository>,
    private val offerPriceRepository: RepositoryStrategy<OfferPriceRepository>,
    private val offerRankRepository: RepositoryStrategy<OfferRankRepository>,
    private val offerSearchService: OfferSearchService
) {
    private val logger = KotlinLogging.logger {}

    fun putOffer(
        id: Long,
        owner: String,
        offer: Offer,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Offer> {

        return supplyAsyncEx(Supplier {
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
            var processedOffer = Offer()
            val saveTiming = measureTimeMillis {
                processedOffer = offerRepository.changeStrategy(strategy).saveOffer(putOffer)
            }
            logger.debug(" - save office $saveTiming")

            val savePriceTiming = measureTimeMillis {
                offerPriceRepository.changeStrategy(strategy).savePrices(processedOffer, offer.offerPrices)
            }
            logger.debug(" - save price $savePriceTiming")

            val deleteByOfferIdTiming = measureTimeMillis {
                offerSearchService.deleteByOfferId(id, strategy)
            }
            logger.debug(" - delete by OfferId $deleteByOfferIdTiming")

            val findByIdTiming = measureTimeMillis {
                processedOffer = offerRepository.changeStrategy(strategy).findById(processedOffer.id)!!
            }
            logger.debug(" - find OfferById $findByIdTiming")
            processedOffer
        })
    }

    fun putBulkOffer(
        owner: String,
        offers: Array<Offer>,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<Long>> {
        return supplyAsyncEx(Supplier {
            offers.map {
                try {
                    putOffer(it.id, owner, it, strategy).get().id
                } catch(err: Throwable) {
                    return@map 0L
                }
            }
        })
    }

    fun shallowUpdateOffer(
        id: Long,
        owner: String,
        offer: Offer,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Offer> {

        return supplyAsyncEx(Supplier {
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
            val processedOffer = offerRepository.changeStrategy(strategy).saveOffer(putOffer)
            offerPriceRepository.changeStrategy(strategy).savePrices(processedOffer, offer.offerPrices)

            offerRepository.changeStrategy(strategy).findById(processedOffer.id)!!
        })
    }

    fun deleteOffer(
        id: Long,
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {

        return supplyAsyncEx(Supplier {
            val deletedId = offerRepository.changeStrategy(strategy).deleteOffer(id, owner)
            if (deletedId == 0L) {
                throw NotFoundException()
            }
            offerSearchService.deleteByOfferId(deletedId, strategy)

            deletedId
        })
    }

    fun deleteOffers(
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Void> {

        return runAsyncEx(Runnable {
            val offerIds = offerRepository.changeStrategy(strategy).findIdsByOwner(owner)

            offerRepository.changeStrategy(strategy).deleteOffers(owner)
            offerRankRepository.changeStrategy(strategy).deleteByOfferIdIn(offerIds)
        })
    }

    fun getOffers(
        id: Long,
        owner: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<Offer>> {

        return supplyAsyncEx(Supplier {
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
        })
    }

    fun getPageableOffers(
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Page<Offer>> {
        return supplyAsyncEx(Supplier {
            offerRepository.changeStrategy(strategy).findAll(page)
        })
    }

    fun getPageableOffersByOwner(
        owner: String,
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Page<Offer>> {
        return supplyAsyncEx(Supplier {
            offerRepository.changeStrategy(strategy).findByOwner(owner, page)
        })
    }

    fun getPageableOffersForMatcher(
        page: PageRequest,
        strategy: RepositoryStrategyType
    ): CompletableFuture<Page<Offer>> {
        return supplyAsyncEx(Supplier {
            offerRepository.changeStrategy(strategy).getAllOffersExceptProducts(page)
        })
    }

    fun getConsumersOffers(
        page: PageRequest,
        syncCompare: Boolean = true,
        syncRules: Boolean = true,
        syncPrices: Boolean = true,
        strategy: RepositoryStrategyType,
        exceptType: Offer.OfferType?
    ): CompletableFuture<Slice<Offer>> {
        return supplyAsyncEx(Supplier {
            offerRepository.changeStrategy(strategy)
                .getAllOffersSlice(page, syncCompare, syncRules, syncPrices, exceptType)
        })
    }

    fun getOfferTotalCount(
        strategy: RepositoryStrategyType
    ): CompletableFuture<Long> {
        return supplyAsyncEx(Supplier {
            offerRepository.changeStrategy(strategy).getTotalCount()
        })
    }

    fun getOfferByOwnerAndTag(
        owner: String,
        tagKey: String,
        strategy: RepositoryStrategyType
    ): CompletableFuture<List<Offer>> {
        return supplyAsyncEx(Supplier {
            offerRepository.changeStrategy(strategy).getOfferByOwnerAndTag(owner, tagKey)
        })
    }
}

