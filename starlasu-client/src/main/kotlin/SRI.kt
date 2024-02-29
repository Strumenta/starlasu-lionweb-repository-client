package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.lionweb.LWNode
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository
import com.strumenta.lwrepoclient.base.addConcept
import com.strumenta.lwrepoclient.base.addContainment
import com.strumenta.lwrepoclient.base.addProperty
import com.strumenta.lwrepoclient.base.lwLanguage
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.model.impl.DynamicNode
import kotlin.reflect.KClass
import com.strumenta.lwrepoclient.base.Multiplicity as LWMultiplicity

class SRI(private val kolasuClient: KolasuClient, val partitionID: String) : SymbolRepository {
    val symbols = mutableListOf<SymbolDescription>()

    val sriNodeID: String
        get() = SRI.sriNodeID(partitionID)

    override fun find(withType: KClass<out Node>): Sequence<SymbolDescription> {
        return symbols.filter { it.types.contains(withType.qualifiedName) }.asSequence()
    }

    override fun load(identifier: String): SymbolDescription? {
        TODO("Not yet implemented")
    }

    override fun store(symbol: SymbolDescription) {
        TODO("Not yet implemented")
    }

    companion object {
        fun fromLionWeb(sriNode: LWNode, kolasuClient: KolasuClient) : SRI {
            require(sriNode.concept == sriConcept)
            require(sriNode.id!!.endsWith(idSuffix))
            val partitionID = sriNode.id!!.removeSuffix(idSuffix)
            val sri = SRI(kolasuClient, partitionID)
            sriNode.getChildrenByContainmentName("entries").forEach { symbolNode ->
                val typesStr = symbolNode.getPropertyValueByName("types") as String
                val symbolDescription = SymbolDescription(
                    symbolNode.getPropertyValueByName("representedIdentifier") as String,
                    symbolNode.getPropertyValueByName("name") as String,
                    if (typesStr.isEmpty()) emptyList() else typesStr.split(":"),
                    emptyMap()
                )
                sri.symbols.add(symbolDescription)
            }
            return sri
        }

        fun sriNodeID(partitionID: String): String {
            return "$partitionID$idSuffix"
        }

        private val idSuffix = "-SRI"
    }

}

fun SRI.toLionWeb() : LWNode {
    val sriNode = DynamicNode(this.sriNodeID, sriConcept)
    this.symbols.forEach { symbol ->
        val symbolNode = DynamicNode(symbol.identifier+"_symbol", symbolConcept)
        symbolNode.setPropertyValueByName("representedIdentifier", symbol.identifier)
        symbolNode.setPropertyValueByName("name", symbol.name)
        symbolNode.setPropertyValueByName("types", symbol.types.joinToString(":"))
        sriNode.addChild(sriConcept.getContainmentByName("entries")!!, symbolNode)
    }
    return sriNode
}

val sriLanguage = lwLanguage("com.strumenta.SymbolResolutionIndex").apply {
    val sri = addConcept("SRI")
    val symbol = addConcept("Symbol")
    symbol.addImplementedInterface(LionCoreBuiltins.getINamed())
    sri.addContainment("entries", symbol, LWMultiplicity.ZERO_TO_MANY)
    symbol.addProperty("representedIdentifier", LionCoreBuiltins.getString())
    symbol.addProperty("types", LionCoreBuiltins.getString())
}
val sriConcept by lazy {
    sriLanguage.getConceptByName("SRI")!!
}
val symbolConcept by lazy {
    sriLanguage.getConceptByName("Symbol")!!
}
