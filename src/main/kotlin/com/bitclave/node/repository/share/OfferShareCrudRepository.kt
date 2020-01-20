package com.bitclave.node.repository.share

import com.bitclave.node.repository.entities.OfferShareData
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
interface OfferShareCrudRepository : CrudRepository<OfferShareData, Long> {

    @Transactional(readOnly = true)
    fun findByOfferOwner(owner: String): List<OfferShareData>

    @Transactional(readOnly = true)
    fun findByOfferOwnerAndAccepted(owner: String, accepted: Boolean): List<OfferShareData>
}
