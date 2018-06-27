package com.bitclave.node.site

import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.repository.account.AccountCrudRepository
import com.bitclave.node.repository.account.AccountRepositoryStrategy
import com.bitclave.node.repository.account.HybridAccountRepositoryImpl
import com.bitclave.node.repository.account.PostgresAccountRepositoryImpl
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.Site
import com.bitclave.node.repository.site.PostgresSiteRepositoryImpl
import com.bitclave.node.repository.site.SiteCrudRepository
import com.bitclave.node.repository.site.SiteRepositoryStrategy
import com.bitclave.node.services.errors.BadArgumentException
import com.bitclave.node.services.errors.NotFoundException
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.SiteService
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
class SiteServiceTest {

    @Autowired
    private lateinit var web3Provider: Web3Provider
    @Autowired
    private lateinit var hybridProperties: HybridProperties

    @Autowired
    protected lateinit var accountCrudRepository: AccountCrudRepository

    @Autowired
    protected lateinit var siteCrudRepository: SiteCrudRepository
    protected lateinit var siteService: SiteService

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"
    private val origin = "www.mysite.com"
    private val account: Account = Account(publicKey)
    protected lateinit var strategy: RepositoryStrategyType

    private val site = Site(
            0,
            origin,
            account.publicKey
    )

    @Before fun setup() {
        val postgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val hybrid = HybridAccountRepositoryImpl(web3Provider, hybridProperties)
        val repositoryStrategy = AccountRepositoryStrategy(postgres, hybrid)
        val accountService = AccountService(repositoryStrategy)
        val siteRepository = PostgresSiteRepositoryImpl(siteCrudRepository)
        val siteRepositoryStrategy = SiteRepositoryStrategy(siteRepository)

        siteService = SiteService(siteRepositoryStrategy)

        strategy = RepositoryStrategyType.POSTGRES
        accountService.registrationClient(account, strategy)
    }

    @Test fun `should be save information of site`() {
        val id = siteService.saveSiteInformation(
                site,
                strategy
        ).get()

        assert(id >= 1L)
    }

    @Test fun `should found information of site by origin`() {
        `should be save information of site`()

        val result = siteService.getSite(origin, strategy).get()

        assert(result.origin == origin)
        assert(result.publicKey == publicKey)
    }

    @Test(expected = NotFoundException::class)
    fun `should not found information of site by origin`() {
        `should be save information of site`()

        try {
            siteService.getSite("www.test.com", strategy).get()
        } catch (e: Exception) {
            throw e.cause!!
        }
    }

    @Test(expected = BadArgumentException::class)
    fun `should be excepted by wrong origin`() {
        val badSiteInfo = Site(
                0,
                "http://www.site.com",
                publicKey
        )
        try {
            siteService.saveSiteInformation(
                    badSiteInfo,
                    strategy
            ).get()

        } catch (e: Exception) {
            throw e.cause!!
        }
    }

}
