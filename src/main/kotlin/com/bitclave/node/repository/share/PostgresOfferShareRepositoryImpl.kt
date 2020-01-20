package com.bitclave.node.repository.share

import com.bitclave.node.repository.entities.OfferShareData
import com.bitclave.node.services.errors.DataNotSavedException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Qualifier("postgres")
class PostgresOfferShareRepositoryImpl(
    val repository: OfferShareCrudRepository
) : OfferShareRepository {

    override fun saveShareData(shareData: OfferShareData) {
        repository.save(shareData) ?: throw DataNotSavedException()
    }

    @Transactional(readOnly = true)
    override fun findByOfferSearchId(id: Long): OfferShareData? {
        return repository.findByIdOrNull(id)
    }

    override fun findByOfferOwnerAndAccepted(offerOwner: String, accepted: Boolean): List<OfferShareData> {
        return repository.findByOfferOwnerAndAccepted(offerOwner, accepted)
    }

    override fun findByOfferOwner(offerOwner: String): List<OfferShareData> {
        return repository.findByOfferOwner(offerOwner)
    }
}
