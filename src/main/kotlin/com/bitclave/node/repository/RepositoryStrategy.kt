package com.bitclave.node.repository

interface RepositoryStrategy<T> {

    fun changeStrategy(type: RepositoryStrategyType): T

}
