package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.semantics.scope.provider.ScopeProvider
import com.strumenta.kolasu.semantics.symbol.provider.SymbolProvider
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository
import com.strumenta.kolasu.semantics.symbol.resolver.SymbolResolver
import com.strumenta.kolasu.traversing.walk

// TODO: move this to code-insight-studio
fun KolasuClient.populateSRI(
    partitionID: String,
    symbolProviderFactory: (nodeIdProvider: NodeIdProvider) -> SymbolProvider,
): SRI {
    val symbolProvider = symbolProviderFactory.invoke(this.idProvider)

    val sri = loadSRI(partitionID)
    val partition = getNode(partitionID) as KNode

    partition.children.forEach { ast ->
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
            sri.symbols.add(symbol)
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
    val partition = getNode(partitionID)
    val scopeProvider = scopeProviderProvider.invoke(sri, idProvider)

    partition.children.forEach { ast ->
        performSymbolResolutionOnAST(ast, scopeProvider)
        this.updateNode(ast)
    }
}
