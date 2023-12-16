package com.strumenta.starlasu.lwrepoclient.lionwebexample

import com.strumenta.starlasu.lwrepoclient.Multiplicity
import com.strumenta.starlasu.lwrepoclient.addConcept
import com.strumenta.starlasu.lwrepoclient.addContainment
import com.strumenta.starlasu.lwrepoclient.lwLanguage
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.LionCoreBuiltins

val propertiesFile : Concept
val property : Concept
val propertiesLanguage = lwLanguage("Properties").apply {
    propertiesFile = addConcept("PropertiesFile")
    property = addConcept("Property")
    property.addImplementedInterface(LionCoreBuiltins.getINamed())
    propertiesFile.addContainment("properties", property, Multiplicity.ZERO_TO_MANY)
}


