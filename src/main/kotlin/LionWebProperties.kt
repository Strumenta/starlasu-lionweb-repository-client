package com.strumenta.starlasu.lwrepoclient

import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.model.impl.DynamicNode
import io.lionweb.lioncore.java.self.LionCore
import kotlin.random.Random

fun lwLanguage(name: String) : Language {
    return Language(name, "language-${name.lowercase()}-id", "language-${name.lowercase()}-key", "1")
}

fun Language.addConcept(name: String) : Concept {
    val concept = Concept(propertiesLanguage, name,
        "${this.id!!.removePrefix("language-").removeSuffix("-id")}-${name}-id",
        "${this.key!!.removePrefix("language-").removeSuffix("-key")}-${name}-key")
    this.addElement(concept)
    return concept
}

enum class Multiplicity {
    SINGLE,
    ZERO_TO_MANY
}

fun Concept.addContainment(name: String, containedConcept: Concept, multiplicity: Multiplicity = Multiplicity.SINGLE) : Containment {
    val containment = Containment().apply {
        this.name = name
        this.id = "${this@addContainment.id!!.removeSuffix("-id")}-${name}-id"
        this.key = "${this@addContainment.key!!.removeSuffix("-key")}-${name}-key"
        this.type = containedConcept
        this.setOptional(when (multiplicity) {
            Multiplicity.SINGLE -> false
            Multiplicity.ZERO_TO_MANY -> true
        })
        this.setMultiple(when (multiplicity) {
            Multiplicity.SINGLE -> false
            Multiplicity.ZERO_TO_MANY -> true
        })
    }
    this.addFeature(containment)
    return containment
}

val propertiesFile : Concept
val property : Concept
val propertiesLanguage = lwLanguage("Properties").apply {
    propertiesFile = addConcept("PropertiesFile")
    property = addConcept("Property")
    property.addImplementedInterface(LionCoreBuiltins.getINamed())
    propertiesFile.addContainment("properties", property, Multiplicity.ZERO_TO_MANY)
}

fun Concept.dynamicNode(nodeId: String = "node-id-rand-${Random.nextInt()}") : DynamicNode {
    return DynamicNode(nodeId, this)
}

//fun DynamicNode.property(name: String, value: Any?) {
//    val feature = this.concept.getFeatureByName(name)
//    if (feature is Property) {
//        this.setPropertyValue(feature, value)
//    } else {
//        throw IllegalStateException()
//    }
//}