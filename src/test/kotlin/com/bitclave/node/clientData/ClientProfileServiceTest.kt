package com.bitclave.node.clientData

import com.bitclave.node.repository.RepositoryType
import com.bitclave.node.repository.data.ClientDataCrudRepository
import com.bitclave.node.repository.data.ClientDataRepositoryStrategy
import com.bitclave.node.repository.data.PostgresClientDataRepositoryImpl
import com.bitclave.node.services.ClientProfileService
import com.bitclave.node.configuration.properties.EthereumProperties
import com.bitclave.node.repository.data.EthClientDataRepositoryImpl
import com.bitclave.node.solidity.generated.AccountContract
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.web3j.crypto.Credentials
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.Web3j
import org.web3j.tx.Contract.GAS_LIMIT
import org.web3j.tx.ManagedTransaction.GAS_PRICE

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ClientProfileServiceTest {

    @Autowired
    protected lateinit var clientDataCrudRepository: ClientDataCrudRepository
    protected lateinit var clientProfileService: ClientProfileService

    protected val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    protected lateinit var data: Map<String, String>
    @Before
    fun setup() {
        data = mapOf("name" to "my name")
        val web3 = Web3j.build(HttpService(EthereumProperties().nodeUrl))
        val credentials = Credentials.create("c87509a1c067bbde78beb793e6fa76530b6382a4c0241e5e4a9ec0a0f44dc0d3") // First PrivKey from ganache-cli
        val accountContract = AccountContract.deploy(web3, credentials, GAS_PRICE, GAS_LIMIT, "0x0").send()

        val postgres = PostgresClientDataRepositoryImpl(clientDataCrudRepository)
        val ethereum = EthClientDataRepositoryImpl(accountContract)
        val strategy = ClientDataRepositoryStrategy(postgres, ethereum)
        clientProfileService = ClientProfileService(strategy)
        strategy.changeStrategy(RepositoryType.POSTGRES)
    }

    @Test
    fun getData() {
        updateData()
        val resultData = clientProfileService.getData(publicKey).get()
        Assertions.assertThat(resultData).isEqualTo(data)
    }

    @Test
    fun updateData() {
        val resultData = clientProfileService.updateData(publicKey, data).get()
        Assertions.assertThat(resultData).isEqualTo(data)
    }

}
