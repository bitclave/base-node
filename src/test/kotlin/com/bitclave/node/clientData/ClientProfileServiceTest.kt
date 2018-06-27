package com.bitclave.node.clientData

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.data.ClientDataCrudRepository
import com.bitclave.node.repository.data.ClientDataRepositoryStrategy
import com.bitclave.node.repository.data.HybridClientDataRepositoryImpl
import com.bitclave.node.repository.data.PostgresClientDataRepositoryImpl
import com.bitclave.node.services.v1.ClientProfileService
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ClientProfileServiceTest {

    @Autowired
    private lateinit var clientDataCrudRepository: ClientDataCrudRepository
    @Autowired
    private lateinit var web3Provider: Web3Provider
    @Autowired
    private lateinit var hybridProperties: HybridProperties

    protected lateinit var clientProfileService: ClientProfileService

    protected val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    protected lateinit var data: Map<String, String>
    protected lateinit var strategy: RepositoryStrategyType

    @Before fun setup() {
        val postgres = PostgresClientDataRepositoryImpl(clientDataCrudRepository)
        val hybrid = HybridClientDataRepositoryImpl(web3Provider, hybridProperties)
        val dataClientRepositoryStrategy = ClientDataRepositoryStrategy(postgres, hybrid)

        clientProfileService = ClientProfileService(dataClientRepositoryStrategy)

        data = mapOf("name" to "my name")

        strategy = RepositoryStrategyType.POSTGRES
    }

    @Test fun `get client raw data by public key`() {
        `update client data by public key`()
        val resultData = clientProfileService.getData(publicKey, strategy).get()
        Assertions.assertThat(resultData).isEqualTo(data)
    }

    @Test fun `update client data by public key`() {
        clientProfileService.updateData(publicKey, data, strategy).get()
    }

    @Test fun `update part of client data by public key`() {
        clientProfileService.updateData(publicKey, data, strategy).get()
        val updatedData = mapOf("email" to "Bob@email.com", "lastname" to "bob-lastname").toMutableMap()
        clientProfileService.updateData(publicKey, updatedData, strategy).get()

        updatedData.putAll(data)

        var result = clientProfileService.getData(publicKey, strategy).get()

        Assertions.assertThat(result.containsKey("name"))
        Assertions.assertThat(result.containsValue(data.getValue("name")))

        Assertions.assertThat(result == updatedData)

        val changeNameMap = mapOf("name" to "Bob")

        clientProfileService.updateData(publicKey, changeNameMap, strategy).get()

        result = clientProfileService.getData(publicKey, strategy).get()

        Assertions.assertThat(result.containsKey("name"))
        Assertions.assertThat(!result.containsValue(data.getValue("name")))
        Assertions.assertThat(result["name"] == "Bob")
        updatedData.putAll(changeNameMap)

        Assertions.assertThat(result == updatedData)
    }

    @Test fun `delete client raw data by public key`() {
        `update client data by public key`()
        clientProfileService.deleteData(publicKey, strategy).get()
        val resultData = clientProfileService.getData(publicKey, strategy).get()
        Assertions.assertThat(resultData).isEmpty()
    }

}
