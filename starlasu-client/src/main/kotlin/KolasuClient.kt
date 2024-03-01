package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.ids.ConstantSourceIdProvider
import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.ids.SourceIdProvider
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.lionweb.LionWebModelConverter
import com.strumenta.kolasu.lionweb.LionWebPartition
import com.strumenta.kolasu.lionweb.PrimitiveValueSerialization
import com.strumenta.kolasu.lionweb.StructuralLionWebNodeIdProvider
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.model.containingProperty
import com.strumenta.kolasu.model.indexInContainingProperty
import com.strumenta.kolasu.semantics.scope.provider.ScopeProvider
import com.strumenta.kolasu.semantics.symbol.provider.SymbolProvider
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository
import com.strumenta.kolasu.semantics.symbol.resolver.SymbolResolver
import com.strumenta.kolasu.traversing.walk
import com.strumenta.lwrepoclient.base.LionWebClient
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.serialization.UnavailableNodePolicy
import io.lionweb.lioncore.java.utils.CommonChecks
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * The logic of assignments of IDs is based on the positions of the elements within the partition,
 * so that the containment of the node within the partition and its index within the containment affect the ID.
 * Eventually we should change that, and consider for example the name of the file and its path relative to the root
 * of the directory imported.
 */
class KolasuClient(val hostname: String = "localhost", val port: Int = 3005, val debug: Boolean = false) {
    /**
     * Exposed for testing purposes
     */
    val nodeConverter = LionWebModelConverter()

    /**
     * This is the logic we use, unless the Node Id was explicitly set. This can be customized, if needed.
     * For example, we may want to use some form of semantic Node ID for certain kinds of Nodes, like qualified names
     * for Class Declarations.
     */
    var baseIdProvider: NodeIdProvider = DefaultLionWebRepositoryNodeIdProvider()

    /**
     * This is the idProvider we concretely use. This consider explicit overrides first, and if they are not
     * present it fals back to the baseIdProvider.
     */
    val idProvider = OverridableNodeIdProvider(this)
    private val lionWebClient = LionWebClient(hostname, port, debug = debug, jsonSerializationProvider = { this.jsonSerialization })

    init {
        lionWebClient.registerLanguage(sriLanguage)
    }

    /**
     * Exposed for testing purposes
     */
    val jsonSerialization: JsonSerialization
        get() {
            return nodeConverter.prepareJsonSerialization(
                JsonSerialization.getStandardSerialization().apply {
                    enableDynamicNodes()
                    unavailableParentPolicy = UnavailableNodePolicy.NULL_REFERENCES
                    unavailableReferenceTargetPolicy = UnavailableNodePolicy.PROXY_NODES
                    registerLanguage(sriLanguage)
                },
            )
        }

    fun registerLanguage(kolasuLanguage: KolasuLanguage) {
        val lionwebLanguage = nodeConverter.exportLanguageToLionWeb(kolasuLanguage)
        lionWebClient.registerLanguage(lionwebLanguage)
    }

    fun <E : Any> registerPrimitiveValueSerialization(
        kClass: KClass<E>,
        primitiveValueSerialization: PrimitiveValueSerialization<E>,
    ) {
        nodeConverter.registerPrimitiveValueSerialization(kClass, primitiveValueSerialization)
    }

    fun getPartitionIDs(): List<String> {
        return lionWebClient.getPartitionIDs()
    }

    /**
     * When this method is called, we calculate the Node ID for the partition using the standard idProvider.
     */
    fun createPartition(kPartition: Node): String {
        if (kPartition.children.isNotEmpty()) {
            throw IllegalArgumentException("When creating a partition, please specify a single node")
        }
        val lwPartition = nodeConverter.exportModelToLionWeb(kPartition, idProvider)
        lionWebClient.createPartition(lwPartition)
        return lwPartition.id!!
    }

    /**
     * When this method is called, we specify an explicitly ID for the Node, and it will be remembered.
     */
    fun createPartition(
        kPartition: Node,
        partitionID: String,
    ): String {
        if (kPartition.children.isNotEmpty()) {
            throw IllegalArgumentException("When creating a partition, please specify a single node")
        }
        idProvider[kPartition] = partitionID
        return createPartition(kPartition)
    }

    /**
     * @param parentId when we store a subtree, we can specify where to attach it by specifying the parentId
     */
    fun storeTree(
        kNode: Node,
        baseId: String,
    ) {
        val lwNode = nodeConverter.exportModelToLionWeb(kNode, StructuralLionWebNodeIdProvider(baseId))
        lionWebClient.storeTree(lwNode)
    }

    fun storeTree(kNode: Node) {
        // Otherwise if we changed the node in the meantime, changes are not stored
        nodeConverter.clearNodesMapping()
        val lwNode = nodeConverter.exportModelToLionWeb(kNode, idProvider)
        lionWebClient.storeTree(lwNode)
    }

    /**
     * The Client will remember the LionWeb IDs of the LionWeb nodes from which
     * this tree has been obtained. Subsequently call to idFor will permit to retrieve
     * such Node IDs.
     */
    fun retrieve(nodeId: String): Node {
        val lwNode = lionWebClient.retrieve(nodeId)
        val kNode = nodeConverter.importModelFromLionWeb(lwNode)
        // TODO ensure this is called by importModelFromLionWeb
        kNode.assignParents()
        kNode.walk().forEach { kNodeIt ->
            // This should be based on the cache
            // TODO create method called getCachedNode(kNode)
            val lwNodeIt = nodeConverter.exportModelToLionWeb(kNodeIt)
            idProvider[kNodeIt] = lwNodeIt.id!!
        }
        return kNode
    }

    /**
     * To be called exactly once, to ensure the Model Repository is initialized.
     * Note that it causes all content of the Model Repository to be lost!
     */
    fun modelRepositoryInit() {
        lionWebClient.modelRepositoryInit()
    }

    /**
     * This operation is not atomic. We hope that no one is changing the parent at the very
     * same time.
     */
    fun <C : KNode, E : KNode> appendTree(
        treeToAppend: KNode,
        container: C,
        containment: KProperty1<C, out Collection<out E>>,
    ): String {
        return appendTree(treeToAppend, idFor(container), containment)
    }

    /**
     * This operation is not atomic. We hope that no one is changing the parent at the very
     * same time.
     *
     * Note that for all the nodes part of treeToAppend we will remember the Node ID associated to
     * them. It will be possible to retrieve it by using idFor.
     */
    fun <C : KNode, E : KNode> appendTree(
        treeToAppend: KNode,
        containerId: String,
        containment: KProperty1<C, out Collection<out E>>,
    ): String {
        val container = lionWebClient.retrieve(containerId)
        val index = container.getChildrenByContainmentName(containment.name).size
        val lwTreeToAppend =
            nodeConverter.exportModelToLionWeb(
                treeToAppend,
                SubTreeLionWebNodeIdProvider(containerId, containment.name, index),
            )
        treeToAppend.walk().forEach { kNode ->
            // This should be based on the cache
            // TODO create method called getCachedNode(kNode)
            val lwNode = nodeConverter.exportModelToLionWeb(kNode)
            idProvider[kNode] = lwNode.id!!
        }
        if (debug) {
            File("lwTreeToAppend.json").writeText(
                nodeConverter.prepareJsonSerialization().serializeTreesToJsonString(lwTreeToAppend),
            )
        }
        lionWebClient.appendTree(lwTreeToAppend, containerId, containment.name)
        return lwTreeToAppend.id!!
    }

    /**
     * Return the Node ID associated to the Node. If the Client has already "seen"
     * the Node before associated to a particular Node ID (either during insertion or retrieval)
     * such Node ID will be returned. Otherwise the Node ID will be calculated based on the Node itself.
     *
     * Note that if you call this method on a Node before inserting it on the repository, we will not know
     * _where_ in the repository you will insert it, therefore the ID you will get would be the one for the
     * Node as "dangling in the void". The Node ID obtained after the insertion could be different!
     */
    fun idFor(kNode: KNode): String {
        return idProvider.id(kNode)
    }

    fun clearNodeIdCache() {
        this.idProvider.clearOverrides()
    }

    fun populateSRI(
        partitionID: String,
        symbolProviderFactory: (nodeIdProvider: NodeIdProvider) -> SymbolProvider,
    ): SRI {
        val symbolProvider = symbolProviderFactory.invoke(this@KolasuClient.idProvider)

        val sri = loadSRI(partitionID)
        val partition = retrieve(partitionID)

        partition.children.forEach { ast ->
            populateSRI(sri, ast, symbolProvider)
        }

        storeSRI(partitionID, sri)
        return sri
    }

    fun performSymbolResolutionOnPartition(
        partitionID: String,
        scopeProviderProvider: (sri: SymbolRepository, nodeIdProvider: NodeIdProvider) -> ScopeProvider,
    ) {
        val sri = loadSRI(partitionID)
        val partition = retrieve(partitionID)
        val scopeProvider = scopeProviderProvider.invoke(sri, idProvider)

        partition.children.forEach { ast ->
            performSymbolResolutionOnAST(ast, scopeProvider)
            storeTree(ast)
        }
    }

    fun nodesByConcept(): Map<KClass<*>, Set<String>> {
        val lionwebResult = lionWebClient.nodesByClassifier()
        val kolasuResult =
            lionwebResult.map { entry ->
                val languageKey = entry.key.languageKey
                val lionWebLanguage = nodeConverter.knownLWLanguages().find { it.key == languageKey }
                if (lionWebLanguage == null) {
                    null
                } else {
                    val lionWebClassifier = lionWebLanguage.elements.find { it.key == entry.key.classifierKey }
                    if (lionWebClassifier is Concept) {
                        val kolasuClass = nodeConverter.getClassifiersToKolasuClassesMapping()[lionWebClassifier]
                        if (kolasuClass == null) {
                            null
                        } else {
                            kolasuClass to entry.value
                        }
                    } else {
                        throw IllegalStateException("Classifier $lionWebClassifier is unexpected, as it is not a Concept")
                    }
                }
            }.filterNotNull().toMap()
        return kolasuResult
    }

    fun loadSRI(partitionID: String): SRI {
        val nodeId = SRI.sriNodeID(partitionID)
        return if (getPartitionIDs().contains(nodeId)) {
            SRI.fromLionWeb(lionWebClient.retrieve(nodeId), this)
        } else {
            // If it does not exist then it is empty
            SRI(this, partitionID)
        }
    }

    private fun storeSRI(
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

    private fun populateSRI(
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
}

/**
 * This logic consider where we plan to insert the Nodes and produce a Node ID considering
 * that context.
 */
private class SubTreeLionWebNodeIdProvider(
    val containerId: String,
    val containmentName: String,
    val containmentIndex: Int,
) :
    NodeIdProvider {
    private val sourceIdProvider: SourceIdProvider = ConstantSourceIdProvider("")

    override fun id(kNode: Node): String {
        val id =
            if (kNode.parent == null) {
                // Here we pretend we have already as parent the container
                val postfix = "${containmentName}_$containmentIndex"
                "${containerId}_$postfix"
            } else {
                val cp = kNode.containingProperty()!!
                val postfix = if (cp.multiple) "${cp.name}_${kNode.indexInContainingProperty()!!}" else cp.name
                "${id(kNode.parent!!)}_$postfix"
            }
        if (!CommonChecks.isValidID(id)) {
            throw IllegalStateException("An invalid LionWeb Node ID has been produced")
        }
        return id
    }
}

/**
 * Identify if a Node is a partition or not. This is based on the type of the Node.
 * Nodes of Partition types should be always and exclusively used as partitions and never be placed within
 * partitions.
 * Conversely nodes of non-Partition types can only used within partitions and never be partitions themselves.
 *
 * TODO: Move to Kolasu
 */
val KNode.isPartition
    get() = this::class.annotations.any { it is LionWebPartition }
