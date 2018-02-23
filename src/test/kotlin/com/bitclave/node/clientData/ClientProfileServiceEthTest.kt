package com.bitclave.node.clientData

import com.bitclave.node.configuration.properties.EthereumProperties
import com.bitclave.node.repository.RepositoryType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.solidity.generated.AccountContract
import com.bitclave.node.solidity.generated.ClientDataContract
import org.junit.After
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired

class ClientProfileServiceEthTest : ClientProfileServiceTest() {

    @Autowired
    private lateinit var web3Provider: Web3Provider
    @Autowired
    private lateinit var ethereumProperties: EthereumProperties

    @Before
    override fun setup() {
        super.setup()

        val contractAccount = ethereumProperties.contracts.account
        val contractClientData = ethereumProperties.contracts.clientData
        val contractStorage = ethereumProperties.contracts.storage

        web3Provider.ethSnapshot()

        val accountContract = AccountContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractAccount.gasPrice,
                contractAccount.gasLimit,
                contractStorage.address
        ).send()

        val clientDataContract = ClientDataContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractClientData.gasPrice,
                contractClientData.gasLimit,
                contractStorage.address
        ).send()

        assert(contractAccount.address == accountContract.contractAddress)
        assert(contractClientData.address == clientDataContract.contractAddress)

        clientDataContract.addKey("name".padEnd(32, Character.MIN_VALUE).toByteArray()).send()
        clientDataContract.addKey("age".padEnd(32, Character.MIN_VALUE).toByteArray()).send()

        strategy.changeStrategy(RepositoryType.ETHEREUM)
    }

    @After
    fun revertEthState() {
        web3Provider.ethRevert()
    }

}
