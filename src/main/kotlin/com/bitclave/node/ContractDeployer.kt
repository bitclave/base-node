package com.bitclave.node

import com.bitclave.node.configuration.properties.HybridContractData
import com.bitclave.node.configuration.properties.HybridProperties
import com.bitclave.node.repository.Web3Provider
import com.bitclave.node.solidity.generated.*
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("test", "local")
class ContractDeployer(
        private val web3Provider: Web3Provider,
        private val hybridProperties: HybridProperties
) {

    private var contractAccount: HybridContractData = hybridProperties.contracts.nameService

    init {
        deploy()
    }

    final fun deploy() {
        web3Provider.hybridSnapshot()

        val nameServiceContract = NameServiceContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractAccount.gasPrice,
                contractAccount.gasLimit
        ).send()

        val storageContract = StorageContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractAccount.gasPrice,
                contractAccount.gasLimit
        ).send()

        val accountContract = AccountContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractAccount.gasPrice,
                contractAccount.gasLimit,
                storageContract.contractAddress
        ).send()

        val clientDataContract = ClientDataContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractAccount.gasPrice,
                contractAccount.gasLimit
        ).send()

        val requestDataContract = RequestDataContract.deploy(
                web3Provider.web3,
                web3Provider.credentials,
                contractAccount.gasPrice,
                contractAccount.gasLimit
        ).send()

        nameServiceContract.setAddressOf(
                "requestData",
                requestDataContract.contractAddress
        ).send()

        nameServiceContract.setAddressOf(
                "account",
                accountContract.contractAddress
        ).send()

        nameServiceContract.setAddressOf(
                "clientData",
                clientDataContract.contractAddress
        ).send()
    }

    fun revertNode() {
        web3Provider.hybridRevert()
    }

}
