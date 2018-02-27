package com.bitclave.node.controllers

import com.bitclave.node.repository.models.Account
import com.bitclave.node.repository.models.SignedRequest
import com.bitclave.node.services.AccountService
import com.bitclave.node.services.ClientProfileService
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/client/")
class ClientProfileController(private val accountService: AccountService,
                              private val profileService: ClientProfileService) {

    /**
     * Returns encrypted data of the user that is identified by the given ID (Public Key).
     * @param publicKey ID (Public Key) of the user in BASE system.
     *
     * @return Map<String, String>. if client not found then empty Map is returned.
     * Http status - 200.
     */
    @RequestMapping(method = [RequestMethod.GET], value = ["{pk}/"])
    fun getData(
            @PathVariable("pk") publicKey: String
    ): CompletableFuture<Map<String, String>> {

        return profileService.getData(publicKey)
    }

    /**
     * Stores user’s personal data in BASE. Note, the data shall be encrypted by
     * the user before it is passed to this API. The API will verify that
     * the request is cryptographically signed by the owner of the public key.
     * @param request {@link SignedRequest} with {@link Map<String, String>} and signature of
     * the message. Map is <key, value> structure, where key and value are strings.
     * Note: “value” field shall be encrypted by the user before sending to this API.
     *
     * @return {@link Map<String,String>} same data from argument. Http status - 200.
     *
     * @exception   {@link AccessDeniedException} - 403
     *              {@link NotFoundException} - 404
     *              {@link BadArgumentException} - 400
     *              {@link DataNotSaved} - 500
     */
    @RequestMapping(method = [RequestMethod.PATCH])
    fun updateData(
            @RequestBody request: SignedRequest<Map<String, String>>
    ): CompletableFuture<Map<String, String>> {

        return accountService.accountBySigMessage(request)
                .thenCompose { account: Account ->
                    profileService.updateData(account.publicKey, request.data!!)
                }
    }

}
