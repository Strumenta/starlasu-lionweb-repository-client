package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.lionweb.LWNode
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository
import com.strumenta.lionweb.kotlin.addConcept
import com.strumenta.lionweb.kotlin.addContainment
import com.strumenta.lionweb.kotlin.addProperty
import com.strumenta.lionweb.kotlin.lwLanguage
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.model.impl.DynamicNode
import io.lionweb.lioncore.java.utils.CommonChecks
import java.lang.IllegalStateException
import kotlin.reflect.KClass
import com.strumenta.lionweb.kotlin.Multiplicity as LWMultiplicity

// TODO: move this to code-insight-studio
class SRI(private val kolasuClient: KolasuClient, val partitionID: String) : SymbolRepository {
    private val symbols = mutableListOf<SymbolDescription>()

    // We can allow shadowing because sometimes we have wrong codebases containing programs that have the same
    // qualified names
    var allowShadowing = false

    fun getEntries(): List<SymbolDescription> {
        return symbols
    }

    fun addSymbol(symbol: SymbolDescription) {
        val matchingSymbol = symbols.find { it.identifier == symbol.identifier }
        if (matchingSymbol != null) {
            if (allowShadowing) {
                return
            } else {
                throw IllegalArgumentException(
                    "The SRI contains a symbol with the same identifier as the one we are trying " +
                        "to add. Existing symbol: $matchingSymbol, symbol being added: $symbol",
                )
            }
        }
        symbols.add(symbol)
    }

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
        fun fromLionWeb(
            sriNode: LWNode,
            kolasuClient: KolasuClient,
        ): SRI {
            require(sriNode.concept == sriConcept)
            require(sriNode.id!!.endsWith(idSuffix))
            val partitionID = sriNode.id!!.removeSuffix(idSuffix)
            val sri = SRI(kolasuClient, partitionID).apply { allowShadowing = true }
            sriNode.getChildrenByContainmentName("entries").forEach { symbolNode ->
                val typesStr = symbolNode.getPropertyValueByName("types") as String
                val symbolDescription =
                    SymbolDescription(
                        symbolNode.getPropertyValueByName("name") as String,
                        symbolNode.getPropertyValueByName("representedIdentifier") as String,
                        if (typesStr.isEmpty()) emptyList() else typesStr.split(":"),
                        emptyMap(),
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

// TODO: move this to code-insight-studio
fun SRI.toLionWeb(): LWNode {
    val sriNode = DynamicNode(this.sriNodeID, sriConcept)
    this.getEntries().forEach { symbol ->
        val id = symbol.identifier + "_symbol"
        if (!CommonChecks.isValidID(id)) {
            throw IllegalStateException("Invalid ID produced for symbol $symbol")
        }
        val symbolNode = DynamicNode(id, symbolConcept)
        symbolNode.setPropertyValueByName("representedIdentifier", symbol.identifier)
        symbolNode.setPropertyValueByName("name", symbol.name)
        symbolNode.setPropertyValueByName("types", symbol.types.joinToString(":"))
        sriNode.addChild(sriConcept.getContainmentByName("entries")!!, symbolNode)
    }
    return sriNode
}

// TODO: move this to code-insight-studio
val sriLanguage =
    lwLanguage("com.strumenta.SymbolResolutionIndex").apply {
        val sri = addConcept("SRI")
        val symbol = addConcept("Symbol")
        symbol.addImplementedInterface(LionCoreBuiltins.getINamed())
        sri.addContainment("entries", symbol, LWMultiplicity.ZERO_TO_MANY)
        symbol.addProperty("representedIdentifier", LionCoreBuiltins.getString())
        symbol.addProperty("types", LionCoreBuiltins.getString())
    }

// TODO: move this to code-insight-studio
val sriConcept by lazy {
    sriLanguage.getConceptByName("SRI")!!
}

// TODO: move this to code-insight-studio
val symbolConcept by lazy {
    sriLanguage.getConceptByName("Symbol")!!
}
