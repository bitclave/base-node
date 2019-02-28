package com.bitclave.node.configuration.gson

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes

class AnnotationExcludeStrategy : ExclusionStrategy {

    override fun shouldSkipField(attributes: FieldAttributes): Boolean {
        return attributes.getAnnotation(Exclude::class.java) != null
    }

    override fun shouldSkipClass(clazz: Class<*>): Boolean {
        return false
    }
}
