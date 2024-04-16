import com.strumenta.lwkotlin.Multiplicity
import com.strumenta.lwkotlin.addConcept
import com.strumenta.lwkotlin.addContainment
import com.strumenta.lwkotlin.lwLanguage
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.LionCoreBuiltins

val propertiesPartition: Concept
val propertiesFile: Concept
val property: Concept
val propertiesLanguage =
    lwLanguage("Properties").apply {
        propertiesPartition = addConcept("PropertiesPartition")
        propertiesFile = addConcept("PropertiesFile")
        property = addConcept("Property")

        propertiesPartition.isPartition = true
        propertiesPartition.addContainment("files", propertiesFile, Multiplicity.ZERO_TO_MANY)
        propertiesFile.addContainment("properties", property, Multiplicity.ZERO_TO_MANY)
        property.addImplementedInterface(LionCoreBuiltins.getINamed())
    }
