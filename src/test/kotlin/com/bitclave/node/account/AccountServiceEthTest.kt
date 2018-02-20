package com.bitclave.node.account

import com.bitclave.node.configuration.properties.EthereumProperties
import com.bitclave.node.repository.RepositoryType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.solidity.generated.AccountContract
import org.junit.After
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired

class AccountServiceEthTest : AccountServiceTest() {

    @Autowired
    private lateinit var web3Provider: Web3Provider
    @Autowired
    private lateinit var ethereumProperties: EthereumProperties

    @Before
    override fun setup() {
        super.setup()

        val contractAccount = ethereumProperties.contracts.account
        val contractStorage = ethereumProperties.contracts.storage

        web3Provider.ethSnapshot()

        AccountContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractAccount.gasPrice,
                contractAccount.gasLimit,
                contractStorage.address
        ).send()

        strategy.changeStrategy(RepositoryType.ETHEREUM)
    }

    @After
    fun revertEthState() {
        web3Provider.ethRevert()
    }

}
