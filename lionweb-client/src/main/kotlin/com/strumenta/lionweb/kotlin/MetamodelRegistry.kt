package com.strumenta.lionweb.kotlin

import io.lionweb.lioncore.java.language.Classifier
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.PrimitiveType
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.model.ClassifierInstance
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.impl.DynamicNode
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.serialization.data.SerializedClassifierInstance
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

/**
 * This object knows about the association between Concepts and Kotlin classes
 * and between PrimitiveTypes and Kotlin classes.
 */
object MetamodelRegistry {
    private val classToConcept = mutableMapOf<KClass<*>, Concept>()
    private val classToPrimitiveType = mutableMapOf<KClass<*>, PrimitiveType>()

    init {
        registerMapping(Node::class, LionCoreBuiltins.getNode())
        registerMapping(String::class, LionCoreBuiltins.getString())
        registerMapping(Int::class, LionCoreBuiltins.getInteger())
        registerMapping(Boolean::class, LionCoreBuiltins.getBoolean())
    }

    fun registerMapping(
        kClass: KClass<out Node>,
        concept: Concept,
    ) {
        classToConcept[kClass] = concept
    }

    fun registerMapping(
        kClass: KClass<*>,
        primitiveType: PrimitiveType,
    ) {
        require(!kClass.isSubclassOf(Node::class))
        classToPrimitiveType[kClass] = primitiveType
    }

    fun getConcept(kClass: KClass<out Node>): Concept? = classToConcept[kClass]

    fun getPrimitiveType(kClass: KClass<out Node>): PrimitiveType? = classToPrimitiveType[kClass]

    fun prepareJsonSerialization(jsonSerialization: JsonSerialization) {
        classToConcept.forEach { (kClass, concept) ->
            jsonSerialization.instantiator.registerCustomDeserializer(concept.id!!) {
                    classifier: Classifier<*>,
                    serializedClassifierInstance: SerializedClassifierInstance,
                    nodes: MutableMap<String, ClassifierInstance<*>>,
                    propertyValues: MutableMap<Property, Any>,
                ->
                val result = kClass.primaryConstructor!!.callBy(emptyMap()) as Node
                if (result is DynamicNode) {
                    result.id = serializedClassifierInstance.id
                }
                result
            }
        }
    }
}
