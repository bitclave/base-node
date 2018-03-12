package com.bitclave.node.repository.share

import com.bitclave.node.repository.models.OfferShareData
import com.bitclave.node.services.errors.DataNotSaved
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresOfferShareRepositoryImpl(
        val repository: OfferShareCrudRepository
) : OfferShareRepository {

    override fun saveShareData(shareData: OfferShareData) {
        repository.save(shareData) ?: throw DataNotSaved()
    }

    override fun findByOwner(owner: String): List<OfferShareData> {
        return repository.findByOfferOwner(owner)
    }

    override fun findByOwnerAndAccepted(owner: String, accepted: Boolean): List<OfferShareData> {
        return repository.findByOfferOwnerAndAccepted(owner, accepted)
    }

    override fun findByOfferIdAndClientId(offerId: Long, clientId: String): OfferShareData? {
        return repository.findByOfferIdAndClientId(offerId, clientId)
    }

}
