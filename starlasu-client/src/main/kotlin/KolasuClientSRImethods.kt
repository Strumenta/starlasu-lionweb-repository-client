package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.lionweb.LWNode
import com.strumenta.kolasu.lionweb.StarLasuLWLanguage
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.semantics.scope.provider.ScopeProvider
import com.strumenta.kolasu.semantics.symbol.provider.SymbolProvider
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository
import com.strumenta.kolasu.semantics.symbol.resolver.SymbolResolver
import com.strumenta.kolasu.traversing.walk
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.utils.CommonChecks
import java.lang.IllegalStateException

fun KolasuClient.getASTRoots(aLWNode: LWNode): Sequence<KNode> {
    val res = mutableListOf<KNode>()

    fun exploreForASTs(aLWNode: LWNode) {
        val isKNode: Boolean = isKolasuConcept(aLWNode.concept)
        if (isKNode) {
            res.add(toKolasuNode(aLWNode))
        } else {
            aLWNode.children.forEach { exploreForASTs(it) }
        }
    }

    exploreForASTs(aLWNode)
    return res.asSequence()
}

// TODO: move this to code-insight-studio

/**
 * @param partitionID this is the partition containing AST nodes
 */
fun KolasuClient.populateSRI(
    partitionID: String,
    symbolProviderFactory: (nodeIdProvider: NodeIdProvider) -> SymbolProvider,
): SRI {
    val symbolProvider = symbolProviderFactory.invoke(this.idProvider)
    val sri = loadSRI(partitionID)
    val partition = getLionWebNode(partitionID)

    getASTRoots(partition).forEach { ast ->
        populateSRI(sri, ast, symbolProvider)
    }

    storeSRI(partitionID, sri)
    return sri
}

// TODO: move this to code-insight-studio
fun KolasuClient.storeSRI(
    partitionID: String,
    sri: SRI,
) {
    if (!getPartitionIDs().contains(SRI.sriNodeID(partitionID))) {
        // we first need to store the partition
        val emptySRI = SRI(this, partitionID)
        lionWebClient.createPartition(emptySRI.toLionWeb())
    }
    lionWebClient.storeTree(sri.toLionWeb())
}

// TODO: move this to code-insight-studio
private fun KolasuClient.populateSRI(
    sri: SRI,
    ast: Node,
    symbolProvider: SymbolProvider,
) {
    ast.walk().forEach { node ->
        val symbol = symbolProvider.symbolFor(node)
        if (symbol != null) {
            if (!CommonChecks.isValidID(symbol.identifier)) {
                throw IllegalStateException("Invalid ID produced for symbol $symbol")
            }
            sri.addSymbol(symbol)
        }
    }
}

private fun performSymbolResolutionOnAST(
    ast: Node,
    scopeProvider: ScopeProvider,
) {
    val symbolResolver = SymbolResolver(scopeProvider)
    symbolResolver.resolve(ast, entireTree = true)
}

fun KolasuClient.performSymbolResolutionOnPartition(
    partitionID: String,
    scopeProviderProvider: (sri: SymbolRepository, nodeIdProvider: NodeIdProvider) -> ScopeProvider,
) {
    val sri = loadSRI(partitionID)
    val partition = getLionWebNode(partitionID)
    val scopeProvider = scopeProviderProvider.invoke(sri, idProvider)

    getASTRoots(partition).forEach { ast ->
        performSymbolResolutionOnAST(ast, scopeProvider)
        this.updateAST(ast)
    }
}

fun isKolasuConcept(concept: Concept): Boolean {
    return concept.allAncestors().contains(StarLasuLWLanguage.ASTNode)
}
