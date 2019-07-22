package com.bitclave.node.controllers.consumers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.Account
import com.bitclave.node.services.v1.AccountService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/v1/consumers/")
class ConsumersAccountController(
    @Qualifier("v1") private val accountService: AccountService
) : AbstractController() {

    private val logger = KotlinLogging.logger {}

    @ApiOperation(
        "Page through accounts",
        response = Account::class,
        responseContainer = "Slice"
    )
    @ApiResponses(
        value = [
            ApiResponse(code = 200, message = "Success", response = Slice::class)
        ]
    )
    @RequestMapping(value = ["accounts"], method = [RequestMethod.GET], params = ["page", "size"])
    fun getConsumersAccounts(
        @ApiParam("Optional page number to retrieve a particular page. If not specified this API retrieves first page.")
        @RequestParam("page", defaultValue = "0")
        page: Int,

        @ApiParam("Optional page size to include number of accounts in a page. Defaults to 100.")
        @RequestParam("size", defaultValue = "100")
        size: Int,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<Slice<Account>> {

        return accountService.getSliceAccounts(
            PageRequest(page, size, Sort(Sort.Order(Sort.Direction.ASC, "publicKey"))),
            getStrategyType(strategy)
        )
            .exceptionally { e ->
                logger.error("Request: getConsumersAccounts/$page/$size raised $e")
                throw e
            }
    }
}
