package com.bitclave.node.repository.account

import com.bitclave.node.configuration.properties.EthereumProperties
import com.bitclave.node.repository.models.Account
import com.bitclave.node.solidity.generated.AccountContract
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Contract.GAS_LIMIT
import org.web3j.tx.ManagedTransaction.GAS_PRICE

@Component
@Qualifier("ethereum")
class EthAccountRepositoryImpl(val contract: AccountContract) : AccountRepository {

//    init {
//        val web3 = Web3j.build(HttpService(EthereumProperties().nodeUrl))
//        val credentials = Credentials.create("c87509a1c067bbde78beb793e6fa76530b6382a4c0241e5e4a9ec0a0f44dc0d3") // First PrivKey from ganache-cli
//        contract = AccountContract.deploy(web3, credentials, GAS_PRICE, GAS_LIMIT, "0x0").send()
//    }

    override fun saveAccount(publicKey: String) {
        contract.registerPublicKey(publicKey).send()
    }

    override fun findByPublicKey(publicKey: String): Account? {
        if (contract.isRegisteredPublicKey(publicKey).send()) {
            return Account(publicKey)
        }
        return null
    }

}
