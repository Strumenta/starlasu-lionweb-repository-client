package com.strumenta.lionweb.kotlin

import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.PrimitiveType
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.model.Node
import java.lang.IllegalStateException
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
        val base = getConceptByName(conceptName)
        if (base != null) {
            return base
        }
        throw IllegalArgumentException("Cannot find Concept $conceptName")
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
                    val containmentType = ConceptsRegistry.getConcept(baseClassifier) ?: throw IllegalStateException()
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
                    val kClass =
                        property.returnType.classifier
                            as KClass<out Node>
                    val containmentType =
                        ConceptsRegistry.getConcept(
                            kClass,
                        ) ?: throw IllegalStateException("Cannot find concept for $kClass")
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