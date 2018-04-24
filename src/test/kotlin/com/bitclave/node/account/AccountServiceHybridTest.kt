package com.bitclave.node.account

import com.bitclave.node.ContractDeployer
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.services.errors.NotImplementedException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

class AccountServiceHybridTest : AccountServiceTest() {

    @Autowired
    private lateinit var contractDeployer: ContractDeployer

    @Before override fun setup() {
        super.setup()
        contractDeployer.deploy()

        strategy = RepositoryStrategyType.HYBRID
    }

    @Test(expected = NotImplementedException::class)
    override fun `delete client`() {
        try {
            super.`delete client`()
        } catch (e: Exception) {
            throw e.cause!!
        }
    }

    @Test override fun `check invalid nonce`() {
        System.out.println("ignore in Ethereum implementation")
    }

    @After
    fun revertHybridState() {
        contractDeployer.revertNode()
    }

}
