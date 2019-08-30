package com.bitclave.node.file

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.account.AccountCrudRepository
import com.bitclave.node.repository.account.AccountRepositoryStrategy
import com.bitclave.node.repository.account.HybridAccountRepositoryImpl
import com.bitclave.node.repository.account.PostgresAccountRepositoryImpl
import com.bitclave.node.repository.file.FileCrudRepository
import com.bitclave.node.repository.file.FileRepositoryStrategy
import com.bitclave.node.repository.file.PostgresFileRepositoryImpl
import com.bitclave.node.repository.entities.Account
import com.bitclave.node.services.errors.NotFoundException
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.FileService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class FileServiceTest {

    @Autowired
    private lateinit var web3Provider: Web3Provider
    @Autowired
    private lateinit var hybridProperties: HybridProperties

    @Autowired
    protected lateinit var accountCrudRepository: AccountCrudRepository

    @Autowired
    protected lateinit var fileCrudRepository: FileCrudRepository
    protected lateinit var fileService: FileService

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    private val account: Account = Account(publicKey)
    protected lateinit var strategy: RepositoryStrategyType

    private val testFile = MockMultipartFile("data", "filename.txt", "text/plain", "some xml".toByteArray())

    @Before
    fun setup() {
        val postgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val hybrid = HybridAccountRepositoryImpl(web3Provider, hybridProperties)
        val repositoryStrategy = AccountRepositoryStrategy(postgres, hybrid)
        val accountService = AccountService(repositoryStrategy)

        val postgresFileRepository = PostgresFileRepositoryImpl(fileCrudRepository)
        val fileServiceStrategy = FileRepositoryStrategy(postgresFileRepository)

        fileService = FileService(fileServiceStrategy)

        strategy = RepositoryStrategyType.POSTGRES
        accountService.registrationClient(account, strategy)
    }

    @Test
    fun `should be create new file`() {
        val result = fileService.saveFile(testFile, account.publicKey, 0, strategy).get()

        assert(result.id >= 1L)
        assertThat(result.publicKey).isEqualTo(account.publicKey)
        assertThat(result.name).isEqualTo("filename.txt")
        assertThat(result.mimeType).isEqualTo("text/plain")
        assertThat(result.size).isEqualTo(8)
    }

    @Test
    fun `should be update created file`() {
        val updateFile = MockMultipartFile(
            "data", "filenameUpdated.txt", "text/plain", "some updated xml".toByteArray()
        )

        val created = fileService.saveFile(testFile, account.publicKey, 0, strategy).get()

        assert(created.id == 1L)

        val updated = fileService.saveFile(updateFile, account.publicKey, created.id, strategy).get()

        assert(updated.id == 1L)
        assertThat(updated.publicKey).isEqualTo(account.publicKey)
        assertThat(updated.name).isEqualTo("filenameUpdated.txt")
        assertThat(updated.mimeType).isEqualTo("text/plain")
        assertThat(updated.size).isEqualTo(16)
    }

    @Test
    fun `should delete existed file`() {
        `should be create new file`()

        val savedResult = fileService.getFile(1, account.publicKey, strategy).get()

        assertThat(savedResult).isNotNull()

        val deletedId = fileService.deleteFile(1, account.publicKey, strategy).get()

        assert(deletedId == 1L)
    }

    @Test(expected = NotFoundException::class)
    fun `should delete existed file and should throw exception with id and public key`() {
        `should be create new file`()

        val savedResult = fileService.getFile(1, account.publicKey, strategy).get()

        assertThat(savedResult).isNotNull()

        val deletedId = fileService.deleteFile(1, account.publicKey, strategy).get()

        assert(deletedId == 1L)

        try {
            fileService.getFile(1, account.publicKey, strategy).get()
        } catch (e: Throwable) {
            throw e.cause!!
        }
    }

    @Test
    fun `should return file by id and owner`() {
        `should be create new file`()
        val savedResult = fileService.getFile(1, account.publicKey, strategy).get()

        assertThat(savedResult).isNotNull()
    }

    @Test(expected = NotFoundException::class)
    fun `should throw exception`() {
        try {
            `should be create new file`()
            fileService.getFile(1, "", strategy).get()
        } catch (e: Throwable) {
            throw e.cause!!
        }
    }
}
