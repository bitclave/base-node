package com.bitclave.node.models

data class OfferInteractionId(
    val offerId: Long,
    val owner: String
) : Comparable<OfferInteractionId> {
    override fun compareTo(other: OfferInteractionId): Int {
        return if (other.offerId == this.offerId && other.owner == this.owner) 1 else 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OfferInteractionId

        if (offerId != other.offerId) return false
        if (owner != other.owner) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offerId.hashCode()
        result = 31 * result + owner.hashCode()
        return result
    }
}
