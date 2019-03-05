package com.bitclave.node.clientData

import com.bitclave.node.ContractDeployer
import com.bitclave.node.repository.RepositoryStrategyType
import org.junit.After
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired

class ClientProfileServiceHybridTest : ClientProfileServiceTest() {

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
