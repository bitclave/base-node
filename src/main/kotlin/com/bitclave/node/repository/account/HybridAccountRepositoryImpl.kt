package com.bitclave.node.repository.account

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.extensions.ECPoint
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.models.Account
import com.bitclave.node.services.errors.NotImplementedException
import com.bitclave.node.solidity.generated.AccountContract
import com.bitclave.node.solidity.generated.NameServiceContract
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.math.BigInteger


@Component
@Qualifier("hybrid")
class HybridAccountRepositoryImpl(
        private val web3Provider: Web3Provider,
        private val hybridProperties: HybridProperties
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
        val ecPoint = ECPoint(account.publicKey)
        if (!contract.isRegisteredPublicKey(ecPoint.affineX).send()) {
            contract.registerPublicKey(ecPoint.affineX, ecPoint.affineY).send()
        }
        contract.setNonceForPublicKeyX(ecPoint.affineX, account.nonce.toBigInteger()).send()
    }

    override fun deleteAccount(publicKey: String) {
        val ecPoint = ECPoint(publicKey)
        contract.unregisterPublicKey(ecPoint.affineX).send()
    }

    override fun findByPublicKey(publicKey: String): Account? {
        val ecPoint = ECPoint(publicKey)
        if (contract.isRegisteredPublicKey(ecPoint.affineX).send()) {
            val nonce = contract.nonceForPublicKeyX(ecPoint.affineX).send() ?: BigInteger.ZERO
            return Account(publicKey, nonce.toLong())
        }
        return null
    }

}
