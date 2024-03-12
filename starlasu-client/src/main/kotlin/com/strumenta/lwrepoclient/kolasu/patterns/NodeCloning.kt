package com.strumenta.lwrepoclient.kolasu.patterns

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PlaceholderElement
import com.strumenta.kolasu.model.PropertyDescription
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.transformation.ASTTransformer
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

fun <N : Node>N.clone(astTransformer: ASTTransformer, values: VariantValues): N {
    if (this is PlaceholderElement) {
        if (this.multiplePlaceholderElement) {
            throw IllegalStateException("This should be prevented. We would need to do the replacement before, in the parent")
        } else {
            return astTransformer.transform(values.nodeValues[this.placeholderName]) as N
        }
    }
    var constructors = this.javaClass.kotlin.constructors
    var constructor: KFunction<*>
    if (constructors.size == 1) {
        constructor = constructors.first()
    } else {
        constructors = constructors.filter { !it.isCyclic(this.javaClass.kotlin) }
        constructor = if (constructors.size > 1 && constructors.any { this.javaClass.kotlin.primaryConstructor == it }) {
            this.javaClass.kotlin.primaryConstructor!!
        } else {
            constructors.first()
        }
    }
    val args: List<Any?> = constructor.parameters.map { parameter ->
        val correspondingProperty: PropertyDescription = this.properties.find {
                property ->
            property.name == parameter.name
        }
            ?: throw IllegalArgumentException("Parameter $parameter for class ${this.javaClass}")
        correspondingProperty.value?.clone(astTransformer, values) ?: null
    }
    try {
        val instance = constructor.call(*(args.toTypedArray())) as N
        instance.assignParents()
        return instance
    } catch (e: IllegalArgumentException) {
        throw RuntimeException("Cloning $this failed", e)
    }
}

private fun <N : Any>N.clone(astTransformer: ASTTransformer, values: VariantValues): N {
    return if (this is Node) {
        this.clone(astTransformer, values)
    } else if (values.simpleValues.containsKey(this)) {
        values.simpleValues[this] as N
    } else if (this is String) {
        this
    } else if (this.javaClass.kotlin.supertypes.map { it.classifier }.contains(Enum::class)) {
        this
    } else if (this is List<*>) {
        this.map {
            if (it is PlaceholderElement && it.multiplePlaceholderElement) {
                values.multipleNodeValues[it.placeholderName]!!.map { astTransformer.transform(it) }
            } else {
                listOf(it?.clone(astTransformer, values))
            }
        }.flatten().toMutableList() as N
    } else if (this is ReferenceByName<*>) {
        ReferenceByName(values.simpleValues[this.name] as? String ?: this.name, this.referred) as N
    } else {
        TODO(this.toString())
    }
}

private fun KType.isCyclic(clazz: KClass<*>): Boolean {
    require(this.classifier != null)
    return if (this.classifier == clazz) {
        true
    } else {
        val typeClass = this.classifier as? KClass<*>
        if (typeClass == null) {
            TODO()
        } else {
            typeClass.supertypes.any { it.classifier == clazz }
        }
    }
}

private fun KFunction<*>.isCyclic(clazz: KClass<*>): Boolean {
    return this.parameters.any {
        !it.isOptional &&
            !it.type.isMarkedNullable && it.type.isCyclic(clazz)
    }
}
