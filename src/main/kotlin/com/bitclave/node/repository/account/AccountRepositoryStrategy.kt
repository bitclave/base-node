package com.bitclave.node.repository.account

import com.bitclave.node.repository.RepositoryStrategy
import com.bitclave.node.repository.RepositoryType
import com.bitclave.node.repository.models.Account
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class AccountRepositoryStrategy(
        @Qualifier("postgres")
        private val postgres: PostgresAccountRepositoryImpl,

        @Qualifier("ethereum")
        private val ethereum: EthAccountRepositoryImpl

) : RepositoryStrategy, AccountRepository {

    private var repository: AccountRepository = postgres

    override fun changeStrategy(type: RepositoryType) {
        repository = when (type) {
            RepositoryType.POSTGRES -> postgres
            RepositoryType.ETHEREUM -> ethereum
        }
    }

    override fun saveAccount(publicKey: String) = repository.saveAccount(publicKey)

    override fun findByPublicKey(publicKey: String): Account? = repository.findByPublicKey(publicKey)

}
