package com.bitclave.node.controllers.v1

import com.bitclave.node.controllers.AbstractController
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.repository.models.services.ExternalService
import com.bitclave.node.repository.models.services.ServiceCall
import com.bitclave.node.services.v1.AccountService
import com.bitclave.node.services.v1.services.ExternalServicesService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/v1/services")
class ExternalServicesController(
    @Qualifier("v1") private val accountService: AccountService,
    @Qualifier("v1") private val externalServicesService: ExternalServicesService
) : AbstractController() {

    @ApiOperation("Execute external api")
    @ApiResponses(
        value = [ApiResponse(code = 200, message = "Success", response = ResponseEntity::class)]
    )
    @RequestMapping(method = [RequestMethod.POST], value = ["/"])
    fun callExternalService(
        @ApiParam("where client sends Signed Request with ServiceCall", required = true)
        @RequestBody
        request: SignedRequest<ServiceCall>,

        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<ResponseEntity<Any>> {
        return accountService.accountBySigMessage(request, getStrategyType(strategy))
            .thenCompose { externalServicesService.externalCall(request.data!!, getStrategyType(strategy)) }
    }

    @ApiOperation("Get information about available external services")
    @ApiResponses(
        value = [ApiResponse(code = 200, message = "Success", response = List::class)]
    )
    @RequestMapping(method = [RequestMethod.GET], value = ["/"])
    fun getExternalServices(
        @ApiParam("change repository strategy", allowableValues = "POSTGRES, HYBRID", required = false)
        @RequestHeader("Strategy", required = false)
        strategy: String?
    ): CompletableFuture<List<ExternalService>> {
        return externalServicesService.findAll(getStrategyType(strategy))
    }
}
