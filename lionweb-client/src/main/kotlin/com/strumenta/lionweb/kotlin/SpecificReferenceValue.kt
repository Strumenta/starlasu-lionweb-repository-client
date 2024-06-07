package com.strumenta.lionweb.kotlin

import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.ReferenceValue
import kotlin.reflect.KClass

class SpecificReferenceValue<T : Node>(val targetClass: KClass<T>) : ReferenceValue() {
    companion object {
        inline fun <reified T : Node> create(
            resolveInfo: String?,
            referred: Node?,
        ): SpecificReferenceValue<T> {
            return SpecificReferenceValue(T::class).apply {
                this.resolveInfo = resolveInfo
                if (referred != null && !T::class.isInstance(referred)) {
                    throw IllegalArgumentException("Incompatible target specified: target $referred (class ${referred.javaClass.canonicalName}) while expected targets are ${targetClass.qualifiedName}")
                }
                this.referred = referred as T?
            }
        }

        inline fun <reified T : Node> createNull(): SpecificReferenceValue<T> {
            return create(null, null)
        }
    }

    override fun getReferred(): T? {
        val value = super.getReferred()
        if (value == null || targetClass.isInstance(value)) {
            return value as T?
        } else {
            throw IllegalStateException("Referred node has an expected type: $value")
        }
    }

    override fun setReferred(referred: Node?) {
        if (referred == null || targetClass.isInstance(referred)) {
            super.setReferred(referred)
        } else {
            throw IllegalArgumentException("Cannot set referred to $referred")
        }
    }
}
