package com.bitclave.node.repository.share

import com.bitclave.node.repository.models.OfferShareData

interface OfferShareRepository {

    fun saveShareData(shareData: OfferShareData)

    fun findByOwner(owner: String): List<OfferShareData>

    fun findByOwnerAndAccepted(owner: String, accepted: Boolean): List<OfferShareData>

    fun findByOfferIdAndClientId(offerId: Long, clientId: String): OfferShareData?

}
