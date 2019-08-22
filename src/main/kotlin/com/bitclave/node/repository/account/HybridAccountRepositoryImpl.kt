package com.bitclave.node.repository.account

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.extensions.ecPoint
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.models.Account
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.solidity.generated.AccountContract
import com.bitclave.node.solidity.generated.NameServiceContract
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.util.Date

@Component
@Qualifier("hybrid")
class HybridAccountRepositoryImpl(
    web3Provider: Web3Provider,
    hybridProperties: HybridProperties
) : AccountRepository {

    private val nameServiceData = hybridProperties.contracts.nameService
    private lateinit var nameServiceContract: NameServiceContract
    private lateinit var contract: AccountContract

    init {
        nameServiceContract = NameServiceContract.load(
            nameServiceData.address,
            web3Provider.web3,
            web3Provider.credentials,
            nameServiceData.gasPrice,
            nameServiceData.gasLimit
        )

        contract = AccountContract.load(
            nameServiceContract.addressOfName("account").send(),
            web3Provider.web3,
            web3Provider.credentials,
            nameServiceData.gasPrice,
            nameServiceData.gasLimit
        )
    }

    override fun saveAccount(account: Account) {
        val ecPoint = ecPoint(account.publicKey)
        if (!contract.isRegisteredPublicKey(ecPoint.affineX).send()) {
            contract.registerPublicKey(ecPoint.affineX, ecPoint.affineY).send()
        }
        contract.setNonceForPublicKeyX(ecPoint.affineX, account.nonce.toBigInteger()).send()
    }

    override fun deleteAccount(publicKey: String) {
        val ecPoint = ecPoint(publicKey)
        contract.unregisterPublicKey(ecPoint.affineX).send()
    }

    override fun findByPublicKey(publicKey: String): Account? {
        val ecPoint = ecPoint(publicKey)
        if (contract.isRegisteredPublicKey(ecPoint.affineX).send()) {
            val nonce = contract.nonceForPublicKeyX(ecPoint.affineX).send() ?: BigInteger.ZERO
            return Account(publicKey, nonce.toLong())
        }
        return null
    }

    override fun findAll(pageable: Pageable): Slice<Account> {
        throw BadArgumentException("This method is not implemented for hybrid")
    }

    override fun findByPublicKey(publicKeys: List<String>): List<Account> {
        val returnArray: MutableList<Account> = mutableListOf()
        publicKeys.forEach { publicKey ->
            val ecPoint = ecPoint(publicKey)
            if (contract.isRegisteredPublicKey(ecPoint.affineX).send()) {
                val nonce = contract.nonceForPublicKeyX(ecPoint.affineX).send() ?: BigInteger.ZERO
                returnArray.add(Account(publicKey, nonce.toLong()))
            }
        }
        return returnArray
    }

    override fun findByCreatedAtAfter(createdAt: Date): List<Account> {
        throw BadArgumentException("This method is not implemented for hybrid")
    }

    override fun getTotalCount(): Long {
        throw BadArgumentException("This method is not implemented for hybrid")
    }
}
