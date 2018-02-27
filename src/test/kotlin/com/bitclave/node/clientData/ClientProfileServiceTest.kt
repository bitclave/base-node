package com.bitclave.node.clientData

import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.data.ClientDataCrudRepository
import com.bitclave.node.repository.data.ClientDataRepositoryStrategy
import com.bitclave.node.repository.data.PostgresClientDataRepositoryImpl
import com.bitclave.node.services.ClientProfileService
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
    protected lateinit var clientDataCrudRepository: ClientDataCrudRepository
    protected lateinit var clientProfileService: ClientProfileService

    protected val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    protected lateinit var data: Map<String, String>
    protected lateinit var strategy: RepositoryStrategyType

    @Before
    fun setup() {
        data = mapOf("name" to "my name")

        val postgres = PostgresClientDataRepositoryImpl(clientDataCrudRepository)
        val repositoryStrategy = ClientDataRepositoryStrategy(postgres)
        clientProfileService = ClientProfileService(repositoryStrategy)

        strategy = RepositoryStrategyType.POSTGRES
    }

    @Test
    fun getData() {
        updateData()
        val resultData = clientProfileService.getData(publicKey, strategy).get()
        Assertions.assertThat(resultData).isEqualTo(data)
    }

    @Test
    fun updateData() {
        val resultData = clientProfileService.updateData(publicKey, data, strategy).get()
        Assertions.assertThat(resultData).isEqualTo(data)
    }

}
