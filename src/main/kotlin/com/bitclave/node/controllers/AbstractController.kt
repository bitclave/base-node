package com.bitclave.node.controllers

import com.bitclave.node.repository.RepositoryStrategyType

abstract class AbstractController {

    protected fun getStrategyType(string: String?): RepositoryStrategyType {
        return if (string.isNullOrEmpty())
            RepositoryStrategyType.POSTGRES
        else
            RepositoryStrategyType.valueOf(string!!)
    }

}
