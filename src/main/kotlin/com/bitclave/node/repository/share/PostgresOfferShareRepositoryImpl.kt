package com.bitclave.node.repository.share

import com.bitclave.node.repository.models.OfferShareData
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("postgres")
class PostgresOfferShareRepositoryImpl(
        val repository: OfferShareCrudRepository
) : OfferShareRepository {

    override fun saveShareData(shareData: OfferShareData) {
        repository.save(shareData) ?: throw DataNotSavedException()
    }

    override fun findByOfferSearchId(id: Long): OfferShareData? {
        return repository.findOne(id)
    }

    override fun findByOfferOwnerAndAccepted(offerOwner: String, accepted: Boolean): List<OfferShareData> {
        return repository.findByOfferOwnerAndAccepted(offerOwner, accepted)
    }

    override fun findByOfferOwner(offerOwner: String): List<OfferShareData> {
        return repository.findByOfferOwner(offerOwner)
    }

}
