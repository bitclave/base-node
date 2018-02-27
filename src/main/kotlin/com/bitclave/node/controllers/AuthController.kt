package com.bitclave.node.controllers

import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.AccountService
import com.bitclave.node.services.errors.AccessDeniedException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController()
@RequestMapping("/")
class AuthController(private val accountService: AccountService) {

    /**
     * Creates a new user in the system, based on the provided information.
     * The API will verify that the request is cryptographically signed by the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link Account} and
     * signature of the message.
     *
     * @return {@link Account}, Http status - 201.
     *
     * @exception   {@link AccessDeniedException} - 403
     *              {@link BadArgumentException} - 400
     *              {@link AlreadyRegisteredException} - 409
     *              {@link DataNotSaved} - 500
     */
    @RequestMapping(method = [RequestMethod.POST], value = ["registration"])
    @ResponseStatus(value = HttpStatus.CREATED)
    fun registration(@RequestBody request: SignedRequest<Account>): CompletableFuture<Account> {
        return accountService.checkSigMessage(request)
                .thenApply { pk ->
                    if (pk != request.data?.publicKey) {
                        throw AccessDeniedException()
                    }
                    pk
                }
                .thenCompose { accountService.registrationClient(request.data!!) }
    }

    /**
     * Verifies if the specified account already exists in the system.
     * The API will verify that the request is cryptographically signed by
     * the owner of the public key.
     * @param request is {@link SignedRequest} where client sends {@link Account}
     * and signature of the message.
     *
     * @return {@link Account}, Http status - 200.
     *
     * @exception   {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     */
    @RequestMapping(method = [RequestMethod.POST], value = ["exist"])
    fun existAccount(@RequestBody request: SignedRequest<Account>): CompletableFuture<Account> {
        return accountService.checkSigMessage(request)
                .thenApply { pk ->
                    if (pk != request.data?.publicKey) {
                        throw AccessDeniedException()
                    }
                    pk
                }
                .thenCompose { accountService.existAccount(request.data!!) }
    }

}
