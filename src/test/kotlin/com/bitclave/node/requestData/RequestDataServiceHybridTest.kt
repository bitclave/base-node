package com.bitclave.node.requestData

import com.bitclave.node.ContractDeployer
import com.bitclave.node.repository.RepositoryStrategyType
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.springframework.beans.factory.annotation.Autowired

@Ignore  // todo return test when will fixed code for Ethereum
class RequestDataServiceHybridTest : RequestDataServiceTest() {

    @Autowired
    private lateinit var contractDeployer: ContractDeployer

    @Before
    override fun setup() {
        super.setup()
        contractDeployer.deploy()
        strategy = RepositoryStrategyType.HYBRID
    }

    @After
    fun revertHybridState() {
        contractDeployer.revertNode()
    }
}
