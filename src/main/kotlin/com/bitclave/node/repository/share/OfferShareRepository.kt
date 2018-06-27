package com.bitclave.node.repository.share

import com.bitclave.node.repository.models.OfferShareData

interface OfferShareRepository {

    fun saveShareData(shareData: OfferShareData)

    fun findByOfferSearchId(id: Long): OfferShareData?

    fun findByOfferOwnerAndAccepted(offerOwner: String, accepted: Boolean): List<OfferShareData>

    fun findByOfferOwner(offerOwner: String): List<OfferShareData>

}
