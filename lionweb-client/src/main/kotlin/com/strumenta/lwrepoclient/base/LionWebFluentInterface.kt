package com.strumenta.lwrepoclient.base

import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.PrimitiveType
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.model.impl.DynamicNode
import kotlin.random.Random

fun lwLanguage(name: String): Language {
    return Language(name, "language-${name.lowercase()}-id", "language-${name.lowercase()}-key", "1")
}

fun Language.addConcept(name: String): Concept {
    val concept = Concept(
        this,
        name,
        "${this.id!!.removePrefix("language-").removeSuffix("-id")}-$name-id",
        "${this.key!!.removePrefix("language-").removeSuffix("-key")}-$name-key"
    )
    this.addElement(concept)
    return concept
}

enum class Multiplicity {
    SINGLE,
    ZERO_TO_MANY
}

fun Concept.addContainment(name: String, containedConcept: Concept, multiplicity: Multiplicity = Multiplicity.SINGLE): Containment {
    val containment = Containment().apply {
        this.name = name
        this.id = "${this@addContainment.id!!.removeSuffix("-id")}-$name-id"
        this.key = "${this@addContainment.key!!.removeSuffix("-key")}-$name-key"
        this.type = containedConcept
        this.setOptional(
            when (multiplicity) {
                Multiplicity.SINGLE -> false
                Multiplicity.ZERO_TO_MANY -> true
            }
        )
        this.setMultiple(
            when (multiplicity) {
                Multiplicity.SINGLE -> false
                Multiplicity.ZERO_TO_MANY -> true
            }
        )
    }
    this.addFeature(containment)
    return containment
}

fun Concept.addProperty(name: String, type: PrimitiveType, multiplicity: Multiplicity = Multiplicity.SINGLE): Property {
    val property = Property().apply {
        this.name = name
        this.id = "${this@addProperty.id!!.removeSuffix("-id")}-$name-id"
        this.key = "${this@addProperty.key!!.removeSuffix("-key")}-$name-key"
        this.type = type
        this.setOptional(
            when (multiplicity) {
                Multiplicity.SINGLE -> false
                Multiplicity.ZERO_TO_MANY -> true
            }
        )
    }
    this.addFeature(property)
    return property
}

fun Concept.dynamicNode(nodeId: String = "node-id-rand-${Random.nextInt()}"): DynamicNode {
    return DynamicNode(nodeId, this)
}
