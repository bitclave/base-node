package com.bitclave.node

import com.bitclave.node.configuration.properties.AccountProperties
import com.bitclave.node.controllers.AuthController
import com.bitclave.node.repository.account.AccountCrudRepository
import com.bitclave.node.repository.models.Account
import com.bitclave.node.utils.Sha3Utils
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@ActiveProfiles("test")
@RunWith(SpringRunner::class)
@SpringBootTest
class AuthControllerTest {

    @Autowired
    lateinit var authController: AuthController

    @Autowired
    lateinit var accountProperties: AccountProperties

    @Autowired
    lateinit var repository: AccountCrudRepository

    private final val clientPublicKey = "038ae411ca2dd59084e11a75240a62ce70669fbcb778c12e7130f385a26e92faf2"
    final val clientHash = "d2607a102a80866340eef985a1a7a5825a99fc17dba672ea92ba7d9efe0742a0"

    @Test
    fun accountSignIn() {
        val salt = accountProperties.salt
        val clientPreparedId = Sha3Utils.stringToSha3Hex("$salt$clientHash")

        authController.signUp(Account("", clientPublicKey, clientHash)).get()

        val account = authController.signIn(Account("", "", clientHash)).get()

        assert(account.id == clientPreparedId)

        repository.deleteAll()
    }

    @Test
    fun accountSignUp() {
        val salt = accountProperties.salt
        val clientPreparedId = Sha3Utils.stringToSha3Hex("$salt$clientHash")

        val account = authController.signUp(Account("", clientPublicKey, clientHash)).get()

        assert(account.id == clientPreparedId)

        repository.deleteAll()
    }

}
