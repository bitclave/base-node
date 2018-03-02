package com.bitclave.node.clientData

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.solidity.generated.*
import org.junit.After
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired

class ClientProfileServiceHybridTest : ClientProfileServiceTest() {

    @Autowired
    private lateinit var web3Provider: Web3Provider
    @Autowired
    private lateinit var hybridProperties: HybridProperties

    @Before
    override fun setup() {
        super.setup()

        val contractAccount = hybridProperties.contracts.nameService

        web3Provider.hybridSnapshot()

        val nameServiceContract = NameServiceContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractAccount.gasPrice,
                contractAccount.gasLimit
        ).send()

        val storageContract = StorageContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractAccount.gasPrice,
                contractAccount.gasLimit
        ).send()

        val clientDataContract = ClientDataContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractAccount.gasPrice,
                contractAccount.gasLimit,
                storageContract.contractAddress
        ).send()

        nameServiceContract.setAddressOf("clientData", clientDataContract.contractAddress).send()

        assert(nameServiceContract.contractAddress == contractAccount.address)

        strategy = RepositoryStrategyType.HYBRID
    }

    @After
    fun revertHybridState() {
        web3Provider.hybridRevert()
    }

}
