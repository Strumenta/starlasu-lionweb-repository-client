package com.strumenta.lwkotlin

import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.PrimitiveType
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.model.HasSettableParent
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.impl.DynamicNode
import java.lang.IllegalStateException
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.superclasses

/**
 * Create a LionWeb Language with the given name.
 */
fun lwLanguage(
    name: String,
    vararg classes: KClass<out Node>,
): Language {
    val cleanedName = name.lowercase().replace('.', '_')
    val language = Language(name, "language-$cleanedName-id", "language-$cleanedName-key", "1")
    language.addConcepts(*classes)
    return language
}

fun Language.addConcept(name: String): Concept {
    val concept =
        Concept(
            this,
            name,
            "${this.id!!.removePrefix("language-").removeSuffix("-id")}-$name-id",
            "${this.key!!.removePrefix("language-").removeSuffix("-key")}-$name-key",
        )
    this.addElement(concept)
    return concept
}

fun Language.addConcepts(vararg conceptClasses: KClass<out Node>) {
    // First we create them all
    val conceptsByClasses = mutableMapOf<KClass<out Node>, Concept>()
    conceptClasses.forEach { conceptClass ->
        val concept =
            addConcept(
                conceptClass.simpleName
                    ?: throw IllegalArgumentException("Given conceptClass has no name"),
            )
        concept.isAbstract = conceptClass.isAbstract
        conceptsByClasses[conceptClass] = concept
        ConceptsRegistry.registerMapping(conceptClass, concept)
    }

    fun searchConcept(conceptName: String): Concept {
        return getConceptByName(conceptName) ?: throw IllegalArgumentException("Cannot find Concept $conceptName")
    }

    // Then we populate them all
    conceptsByClasses.forEach { conceptClass, concept ->
        conceptClass.superclasses.forEach { superClass ->
            when {
                superClass == BaseNode::class -> Unit // Nothing to do
                superClass.java.isInterface -> Unit
                else -> {
                    val extendedConcept = conceptsByClasses[superClass]
                    if (extendedConcept == null) {
                        throw IllegalStateException("Cannot handle superclass $superClass for concept class $conceptClass")
                    } else {
                        concept.extendedConcept = extendedConcept
                    }
                }
            }
        }

        conceptClass.declaredMemberProperties.filter { it.annotations.none { it is Implementation } }.forEach { property ->
            when (property.returnType.classifier) {
                List::class -> {
                    val baseClassifier = property.returnType.arguments[0].type!!.classifier!! as KClass<out Node>
                    val containmentType = searchConcept(baseClassifier.simpleName!!)
                    concept.addContainment(property.name, containmentType, Multiplicity.ZERO_TO_MANY)
                }
                String::class -> {
                    concept.addProperty(property.name, LionCoreBuiltins.getString(), Multiplicity.SINGLE)
                }
                Int::class -> {
                    concept.addProperty(property.name, LionCoreBuiltins.getInteger(), Multiplicity.SINGLE)
                }
                Boolean::class -> {
                    concept.addProperty(property.name, LionCoreBuiltins.getBoolean(), Multiplicity.SINGLE)
                }
                else -> {
                    val containmentType = searchConcept((property.returnType.classifier as KClass<out Node>).simpleName!!)
                    concept.addContainment(property.name, containmentType, Multiplicity.SINGLE)
                }
            }
        }
    }
}

enum class Multiplicity {
    SINGLE,
    ZERO_TO_MANY,
}

fun Concept.addContainment(
    name: String,
    containedConcept: Concept,
    multiplicity: Multiplicity = Multiplicity.SINGLE,
): Containment {
    val containment =
        Containment().apply {
            this.name = name
            this.id = "${this@addContainment.id!!.removeSuffix("-id")}-$name-id"
            this.key = "${this@addContainment.key!!.removeSuffix("-key")}-$name-key"
            this.type = containedConcept
            this.setOptional(
                when (multiplicity) {
                    Multiplicity.SINGLE -> false
                    Multiplicity.ZERO_TO_MANY -> true
                },
            )
            this.setMultiple(
                when (multiplicity) {
                    Multiplicity.SINGLE -> false
                    Multiplicity.ZERO_TO_MANY -> true
                },
            )
        }
    this.addFeature(containment)
    return containment
}

fun Concept.addProperty(
    name: String,
    type: PrimitiveType,
    multiplicity: Multiplicity = Multiplicity.SINGLE,
): Property {
    val property =
        Property().apply {
            this.name = name
            this.id = "${this@addProperty.id!!.removeSuffix("-id")}-$name-id"
            this.key = "${this@addProperty.key!!.removeSuffix("-key")}-$name-key"
            this.type = type
            this.setOptional(
                when (multiplicity) {
                    Multiplicity.SINGLE -> false
                    Multiplicity.ZERO_TO_MANY -> true
                },
            )
        }
    this.addFeature(property)
    return property
}

/**
 * Create a Dynamic Node with the given Concept and a random node ID.
 */
fun Concept.dynamicNode(nodeId: String = "node-id-rand-${Random.nextInt()}"): DynamicNode {
    return DynamicNode(nodeId, this)
}

fun <N> N.withParent(parent: Node?): N where N : Node, N : HasSettableParent {
    this.setParent(parent)
    return this
}

fun <N:Node>Node.walkDescendants(kClass: KClass<N>): Sequence<N> {
    val results = mutableListOf<N>()
    this.children.forEach { child ->
        if (kClass.isInstance(child)) {
            results.add(child as N)
        }
        results.addAll(child.walkDescendants(kClass))
    }
    return results.asSequence()
}
