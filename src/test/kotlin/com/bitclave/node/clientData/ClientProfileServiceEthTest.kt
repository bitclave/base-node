package com.bitclave.node.clientData

import com.bitclave.node.configuration.properties.EthereumProperties
import com.bitclave.node.repository.RepositoryType
import com.bitclave.node.repository.Web3Provider
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

        val contractClientData = ethereumProperties.contracts.clientData
        val contractStorage = ethereumProperties.contracts.storage

        web3Provider.ethSnapshot()

        ClientDataContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractClientData.gasPrice,
                contractClientData.gasLimit,
                contractStorage.address
        ).send()

        strategy.changeStrategy(RepositoryType.ETHEREUM)
    }

    @After
    fun revertEthState() {
        web3Provider.ethRevert()
    }

}
