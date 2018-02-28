package com.bitclave.node.clientData

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.solidity.generated.AccountContract
import com.bitclave.node.solidity.generated.ClientDataContract
import com.bitclave.node.solidity.generated.RequestDataContract
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

        val contractAccount = hybridProperties.contracts.account
        val contractClientData = hybridProperties.contracts.clientData
        val contractRequestData = hybridProperties.contracts.requestData
        val contractStorage = hybridProperties.contracts.storage

        web3Provider.hybridSnapshot()

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

        val requestDataContract = RequestDataContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractClientData.gasPrice,
                contractClientData.gasLimit,
                contractStorage.address
        ).send()

        assert(contractAccount.address == accountContract.contractAddress)
        assert(contractClientData.address == clientDataContract.contractAddress)
        assert(contractRequestData.address == requestDataContract.contractAddress)

        clientDataContract.addKey("name".padEnd(32, Character.MIN_VALUE).toByteArray()).send()
        clientDataContract.addKey("age".padEnd(32, Character.MIN_VALUE).toByteArray()).send()

        strategy = RepositoryStrategyType.HYBRID
    }

    @After
    fun revertHybridState() {
        web3Provider.hybridRevert()
    }

}
