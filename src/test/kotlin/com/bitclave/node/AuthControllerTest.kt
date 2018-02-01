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

    val shA3Result = "a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a"
    private final val clientPrivateKey = "MIIEogIBAAKCAQEA1+cyTHP4varOPrILWk5QUOHoU81Xw7oqvEwVEE0HhixdUCkFDQfxdg4ubk7q/I16aP9HxBOpRcBwk9CrdI4CVKOOAPqs5rLyVp8opkYvpl8v49XiLEj5PyipKVCSMK1ow5mtAc3Kfx5ZgWL/VisUaIvYji1tHSZLodXZZMEF45GiFAjDVzCjjkwBPhixRSX8/Ewgve4XO0uIgQi6cNn72kUOQqnC3xURuDK0yPZLYsKRzoZ1LFKZ/X6shcsDYt/TI4a+EFUQRFjgy6PqmgmEcGBBZZ5j7XAB82g6G50+zmLBcmQLGFCXp/qlfIByO4gXqTu/keQ/lH7v+aD8qvaTWwIDAQABAoIBAFHtfIvAckJe33axD1fMYfPfVc7/G++85FyhrliKMnG6wgoaFr2UJw96YCohrk/5y9quDGieYVyDovSkeXt4K/A8RDWg6h19CDLQoJdACPLkKgMV9YaChnpuDosL9P2dwCSBW0JU+iN0u9g6XdQv8sWdU6bYHBC0qHyX7q+qA3cP6PY6VuuXTVlsnlx4x++ZDULAqWO64D7sYdOwPukg8b2g6g+q8RvnlHuf8AIcXKES2jua4hMQbVFxZQF4klpCdFq8pJpRd87HuqN7cuvcX/QGvl9Te6XDJ+sTY+yINDkp4qWiPCRT9e5OPZNnrO1zg/R9q40xjl+QXY6EmRbYYAkCgYEA7b36BPcQroqQhI0K9+o/bD1kJk2LdCBk+TmuU+5G6pEy+Xk5Q99rSzz0JaKkkha38ErcRGe4WfbBo+59bUSTsfx4yrktAQxoHDURpPOmh6rN8+u/wcOv6SHBDMRfxdMmxm2ZFDobJ7KBeM2gyodjvT1XboUtroqy87I7sINosNcCgYEA6HvdN82zl+ig9jiPw2Cibmg7Y3wQ8dXPwDnGUODuI6zAGnvVWR5aRW86HbB7DoF7kqfLm6FCHqea0nonrPZb33cHNFt9UAQtUqcU+vl57zspjCJx2OygghUYg24Hk3bCrb8/iQP8h581VrTknQRrfY36nOLzFCvp41vEDATJbR0CgYBpwlNQlh/jpVx4us2gT6J7IixFJcXdmPOSMuisCZmei40jkZu558+xHqJ1g462NkkJKiQmggVub+jgZh2B2Dnjua56cDnm93/w3GLHAGSagY8w7TTWm5jLmupoJYt9U1sydlrctqSR2kTiWocm0We7LWiRj8ksliLZafZHpY2S7QKBgHi4cPvcLcjQ/Fw+rLBIirAIIeab8vSRESsBFTDETARsXOijqvp232s0wZnPHWc/51oVFY4/42Slpahr1BRlbPmSBkSyyVR2hs5ngmZ8i0uljgJ9lF6PKR3DNJJy3S4+ugKcz7InRsdp7bmTfy7lr6dnxV4YHx83WmE8MoYHKk0lAoGAXivqoF4cJsRUsuyfb/6j9JAsU/0IdVG5r55WBDShGgph2deJ7eHfbysIDXYxbIsTASV0B++F2FlB9fO0m5oLnoAubUFeawhBZiZN0j+9MhXCUkL/CVolvntVHl1gMRWc/TubTIOJTRpHdhGT57GdWrpm7Y3CMAN81yX9SohLCbk="
    private final val clientPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1+cyTHP4varOPrILWk5QUOHoU81Xw7oqvEwVEE0HhixdUCkFDQfxdg4ubk7q/I16aP9HxBOpRcBwk9CrdI4CVKOOAPqs5rLyVp8opkYvpl8v49XiLEj5PyipKVCSMK1ow5mtAc3Kfx5ZgWL/VisUaIvYji1tHSZLodXZZMEF45GiFAjDVzCjjkwBPhixRSX8/Ewgve4XO0uIgQi6cNn72kUOQqnC3xURuDK0yPZLYsKRzoZ1LFKZ/X6shcsDYt/TI4a+EFUQRFjgy6PqmgmEcGBBZZ5j7XAB82g6G50+zmLBcmQLGFCXp/qlfIByO4gXqTu/keQ/lH7v+aD8qvaTWwIDAQAB"
    final val clientHash = Sha3Utils.stringToSha3Hex("$clientPrivateKey$clientPublicKey")

    @Test
    fun isValidSha3Function() {
        assert(Sha3Utils.stringToSha3Hex("") == shA3Result)
    }

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
