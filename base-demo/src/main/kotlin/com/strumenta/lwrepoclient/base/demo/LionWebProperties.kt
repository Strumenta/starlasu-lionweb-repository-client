package com.strumenta.lwrepoclient.base.demo

import com.strumenta.lwrepoclient.base.Multiplicity
import com.strumenta.lwrepoclient.base.addConcept
import com.strumenta.lwrepoclient.base.addContainment
import com.strumenta.lwrepoclient.base.lwLanguage
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.LionCoreBuiltins

val propertiesFile: Concept
val property: Concept
val propertiesLanguage = lwLanguage("Properties").apply {
    propertiesFile = addConcept("PropertiesFile")
    property = addConcept("Property")
    property.addImplementedInterface(LionCoreBuiltins.getINamed())
    propertiesFile.addContainment("properties", property, Multiplicity.ZERO_TO_MANY)
}
