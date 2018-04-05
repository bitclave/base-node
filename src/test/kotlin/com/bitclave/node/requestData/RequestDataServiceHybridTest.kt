package com.bitclave.node.requestData

import com.bitclave.node.ContractDeployer
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.services.errors.NotImplementedException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

class RequestDataServiceHybridTest : RequestDataServiceTest() {

    @Autowired
    private lateinit var contractDeployer: ContractDeployer

    @Before override fun setup() {
        super.setup()
        contractDeployer.deploy()
        strategy = RepositoryStrategyType.HYBRID
    }

    @Test(expected = NotImplementedException::class)
    override fun `delete response and requests by From and To`() {
        try {
            super.`delete response and requests by From and To`()
        } catch (e: Exception) {
            throw e.cause!!
        }
    }

    @After fun revertHybridState() {
        contractDeployer.revertNode()
    }

}