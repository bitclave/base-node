package com.bitclave.node.requestData

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.solidity.generated.AccountContract
import com.bitclave.node.solidity.generated.ClientDataContract
import com.bitclave.node.solidity.generated.FacadeContract
import com.bitclave.node.solidity.generated.RequestDataContract
import org.junit.After
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired

class RequestDataServiceHybridTest : RequestDataServiceTest() {

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

        val facadeContract = FacadeContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractAccount.gasPrice,
                contractAccount.gasLimit
        ).send()

        val storageAddress = facadeContract.storageContract().send()
        val accountAddress = facadeContract.account().send()
        val clientDataAddress = facadeContract.clientData().send()
        val requestDataAddress = facadeContract.requestData().send()

        assert(contractStorage.address == storageAddress)
        assert(contractAccount.address == accountAddress)
        assert(contractClientData.address == clientDataAddress)
        assert(contractRequestData.address == requestDataAddress)

        strategy = RepositoryStrategyType.HYBRID
    }

    @After
    fun revertHybridState() {
        web3Provider.hybridRevert()
    }

}