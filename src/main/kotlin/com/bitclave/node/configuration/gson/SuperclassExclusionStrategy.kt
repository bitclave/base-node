package com.bitclave.node.configuration.gson

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import java.lang.reflect.Field

class SuperclassExclusionStrategy : ExclusionStrategy {
    override fun shouldSkipClass(arg0: Class<*>): Boolean {
        return false
    }

    override fun shouldSkipField(fieldAttributes: FieldAttributes): Boolean {
        val fieldName = fieldAttributes.name
        val theClass = fieldAttributes.declaringClass

        return isFieldInSuperclass(theClass, fieldName)
    }

    private fun isFieldInSuperclass(subclass: Class<*>, fieldName: String): Boolean {
        var superclass: Class<*>? = subclass.superclass
        var field: Field?

        while (superclass != null) {
            field = getField(superclass, fieldName)

            if (field != null)
                return true

            superclass = superclass.superclass
        }

        return false
    }

    private fun getField(theClass: Class<*>, fieldName: String): Field? {
        try {
            return theClass.getDeclaredField(fieldName)
        } catch (e: Exception) {
            return null
        }

    }
}
