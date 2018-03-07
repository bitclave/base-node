package com.bitclave.node.search

import com.bitclave.node.repository.RepositoryStrategyType
import com.bitclave.node.repository.account.AccountCrudRepository
import com.bitclave.node.repository.account.AccountRepositoryStrategy
import com.bitclave.node.repository.account.PostgresAccountRepositoryImpl
import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SearchRequest
import com.bitclave.node.repository.search.PostgresSearchRequestRepositoryImpl
import com.bitclave.node.repository.search.SearchRequestCrudRepository
import com.bitclave.node.repository.search.SearchRequestRepositoryStrategy
import com.bitclave.node.services.AccountService
import com.bitclave.node.services.SearchRequestService
import org.assertj.core.api.Assertions.assertThat
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
class SearchRequestServiceTest {

    @Autowired
    protected lateinit var accountCrudRepository: AccountCrudRepository

    @Autowired
    protected lateinit var searchRequestCrudRepository: SearchRequestCrudRepository
    protected lateinit var searchRequestService: SearchRequestService

    private val publicKey = "02710f15e674fbbb328272ea7de191715275c7a814a6d18a59dd41f3ef4535d9ea"

    private val account: Account = Account(publicKey)
    protected lateinit var strategy: RepositoryStrategyType

    private val searchRequest = SearchRequest(
            0,
            account.publicKey,
            mapOf("car" to "true", "color" to "red")
    )

    @Before fun setup() {
        val postgres = PostgresAccountRepositoryImpl(accountCrudRepository)
        val repositoryStrategy = AccountRepositoryStrategy(postgres)
        val accountService = AccountService(repositoryStrategy)
        val searchRequestRepository = PostgresSearchRequestRepositoryImpl(searchRequestCrudRepository)
        val requestRepositoryStrategy = SearchRequestRepositoryStrategy(searchRequestRepository)

        searchRequestService = SearchRequestService(requestRepositoryStrategy)

        strategy = RepositoryStrategyType.POSTGRES
        accountService.registrationClient(account, strategy)
    }

    @Test fun `should be create new search request`() {
        val result = searchRequestService.createSearchRequest(
                account.publicKey,
                searchRequest,
                strategy
        ).get()

        assert(result.id >= 1L)
        assertThat(result.owner).isEqualTo(account.publicKey)
        assertThat(result.tags).isEqualTo(searchRequest.tags)
    }

    @Test fun `should delete existed search request`() {
        `should be create new search request`()

        var savedListResult = searchRequestService.getSearchRequests(1, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(1)
        assertThat(savedListResult[0]).isEqualToIgnoringGivenFields(searchRequest, "id")

        val deletedId = searchRequestService.deleteSearchRequest(1, account.publicKey, strategy).get()

        assert(deletedId == 1L)

        savedListResult = searchRequestService.getSearchRequests(1, account.publicKey, strategy).get()
        assertThat(savedListResult.size).isEqualTo(0)
    }

    @Test fun `should return search requests by id and owner`() {
        `should be create new search request`()
        `should be create new search request`()

        var result = searchRequestService.getSearchRequests(1, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualToIgnoringGivenFields(searchRequest, "id")

        result = searchRequestService.getSearchRequests(2, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0]).isEqualToIgnoringGivenFields(searchRequest, "id")

        result = searchRequestService.getSearchRequests(3, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(0)
    }

    @Test fun `should return search requests by owner`() {
        `should be create new search request`()
        `should be create new search request`()

        val result = searchRequestService.getSearchRequests(0, account.publicKey, strategy).get()
        assertThat(result.size).isEqualTo(2)
        assert(result[0].id == 1L)
        assert(result[1].id == 2L)
        assertThat(result[0]).isEqualToIgnoringGivenFields(searchRequest, "id")
        assertThat(result[1]).isEqualToIgnoringGivenFields(searchRequest, "id")
    }

}
