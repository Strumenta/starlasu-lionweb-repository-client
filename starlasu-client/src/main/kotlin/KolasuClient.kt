package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.ids.IDLogic
import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.ids.NonRootCoordinates
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.lionweb.LWNode
import com.strumenta.kolasu.lionweb.LionWebModelConverter
import com.strumenta.kolasu.lionweb.LionWebRootSource
import com.strumenta.kolasu.lionweb.PrimitiveValueSerialization
import com.strumenta.kolasu.lionweb.isPartition
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.traversing.walkDescendants
import com.strumenta.lwrepoclient.base.LionWebClient
import com.strumenta.lwrepoclient.base.debugFileHelper
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.serialization.UnavailableNodePolicy
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Partitions are top level entries in the repository. While they are still nodes, they must be dealt with through
 * specific methods.
 *
 * The main aspect to consider is how IDs are attributed to nodes. We distinguish two cases:
 * - Dependent ID Nodes (DIN): these are nodes which ID depends on their position in the tree (i.e., looking
 *   at the parents and nodes above). In other words, changing their parent or any ancestor of their parent, will
 *   change their ID.
 * - Independent ID Nodes (IIN) Nodes which can get an ID, based purely on themselves (i.e., without considering
 *   their parent or any ancestor
 *
 * "Container" nodes such as partitions or other nodes created to organize ASTs (e.g., to represent files and
 * directories) should be made as IIN. Also, the roots of ASTs should be made as IIN. All other nodes can be treated
 * as DIN.
 *
 * The problem with IIN is that we must create the conditions so that we get the same ID for each of them when we store
 * them and when we retrieve them from the LionWeb Repository, irrespectively of the fact that we store them directly
 * (or we store their parent or any ancestor) and that we retrieve them directly (or we retrieve their parent or any
 * ancestor).
 *
 * For a node to be IIN it should either (i) be a partition, (ii) being reported as being a source base node type,
 * or (iii) implement IDLogic.
 */
class KolasuClient(val hostname: String = "localhost", val port: Int = 3005, val debug: Boolean = false) {
    val sourceBasedNodeTypes = mutableSetOf<KClass<*>>()

    /**
     * Exposed for testing purposes
     */
    val nodeConverter = LionWebModelConverter()

    /**
     * This is the logic we use to assign Node IDs. This can be customized, if needed.
     * For example, we may want to use some form of semantic Node ID for certain kinds of Nodes, like qualified names
     * for Class Declarations.
     */
    val idProvider: NodeIdProvider = DefaultLionWebRepositoryNodeIdProvider(sourceBasedNodeTypes)

    internal val lionWebClient =
        LionWebClient(
            hostname,
            port,
            debug = debug,
            jsonSerializationProvider = { this.jsonSerialization },
        )

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

    //
    // Configuration
    //

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

    //
    // Operation on partitions
    //

    fun getPartitionIDs(): List<String> {
        return lionWebClient.getPartitionIDs()
    }

    fun partitionExist(kPartition: Node): Boolean {
        require(kPartition.isPartition) {
            "The given Node is not of partition type"
        }
        return partitionExist(idFor(kPartition))
    }

    fun partitionExist(partitionID: String): Boolean {
        return getPartitionIDs().contains(partitionID)
    }

    fun partitionNotExist(kPartition: Node): Boolean {
        return !partitionExist(kPartition)
    }

    /**
     * We create the partition and returns its ID.
     *
     * The node specified should be of partition type.
     *
     * The node should not have children. If you want to create a partition with children, first create it without
     * children and then call updatePartition.
     *
     * The partition should not already exist.
     */
    fun createPartition(kPartition: Node): String {
        require(kPartition.isPartition) {
            "The given Node is not of partition type"
        }
        require(partitionNotExist(kPartition)) {
            "Partition already exist, cannot be created again"
        }
        if (kPartition.children.isNotEmpty()) {
            throw IllegalArgumentException("When creating a partition, please specify a single node")
        }
        val lwPartition = toLionWeb(kPartition)
        lionWebClient.createPartition(lwPartition)
        return lwPartition.id!!
    }

    /**
     * We update the partition and returns its ID.
     *
     * The node specified should be of partition type.
     *
     * The partition should already exist.
     */
    fun updatePartition(kPartition: Node): String {
        require(kPartition.isPartition) {
            "The given Node is not of partition type"
        }
        require(partitionNotExist(kPartition)) {
            "Partition does not exist, cannot be updated"
        }
        val lwPartition = toLionWeb(kPartition)
        lionWebClient.storeTree(lwPartition)
        return lwPartition.id!!
    }

    /**
     * Consider this will retrieve the partition and all the roots it contains.
     * This may mean a very large amount of data.
     */
    fun retrievePartition(nodeID: String): KNode {
        val lwNode = lionWebClient.retrieve(nodeID)
        return nodeConverter.importModelFromLionWeb(lwNode) as KNode
    }

    fun deletePartition(kPartition: Node) {
        require(kPartition.isPartition) {
            "The given Node is not of partition type"
        }
        require(partitionExist(kPartition)) {
            "Partition already exist, cannot be created again"
        }
        deletePartition(idFor(kPartition))
    }

    fun deletePartition(partitionId: String) {
        require(partitionExist(partitionId)) {
            "Partition does not exist"
        }
        lionWebClient.deletePartition(partitionId)
    }

    //
    // Operation on non-partition
    //

    /**
     * Here node means "non partition node".
     */
    fun nodeExist(kNode: KNode): Boolean {
        return nodeExist(idFor(kNode))
    }

    /**
     * Here node means "non partition node".
     */
    fun nodeExist(nodeId: String): Boolean {
        return nodeExistWithExplanation(nodeId) == null
    }

    fun nodeExistWithExplanation(kNode: KNode): String? {
        return nodeExistWithExplanation(idFor(kNode))
    }

    /**
     * Here node means "non partition node".
     */
    fun nodeExistWithExplanation(nodeId: String): String? {
        if (!lionWebClient.isNodeExisting(nodeId)) {
            return "Node with ID $nodeId not found"
        }
        val parentId = lionWebClient.getParentId(nodeId)
        return if (parentId == null) {
            "Node with ID $nodeId has null parent, so it is a partition and not a normal node"
        } else {
            null
        }
    }

    /**
     * Here node means "non partition node".
     */
    fun nodeNotExist(nodeId: String): Boolean {
        return !nodeExist(nodeId)
    }

    /**
     * Here node means "non partition node".
     *
     * This method should be used only with IIN.
     */
    fun createNode(
        kNode: Node,
        kContainer: Node,
        containment: KProperty1<*, *>,
    ): String {
        return createNode(kNode, idFor(kContainer), containment)
    }

    /**
     * Here node means "non partition node".
     *
     * This method should be used only with IIN.
     */
    fun createNode(
        kNode: Node,
        containerID: String,
        containment: KProperty1<*, *>,
    ): String {
        when {
            isIIN(kNode) -> {
                requireIINode(kNode, "createNode")
                val lwTreeToAppend = toLionWeb(kNode, containerID, containment.name)
                debugFile("createNode-${lwTreeToAppend.id}.json") {
                    nodeConverter.prepareJsonSerialization().serializeTreesToJsonString(lwTreeToAppend)
                }
                lionWebClient.appendTree(lwTreeToAppend, containerID, containment.name)
                return lwTreeToAppend.id!!
            }
            else -> {
                throw IllegalStateException(
                    "CreateNode should be used only for nodes that can calculate their own ID independently from " +
                        "their position. Instead we got $kNode (class: ${kNode.javaClass.canonicalName})",
                )
            }
        }
    }

    /**
     * Here node means "non partition node".
     */
    fun updateNode(kNode: KNode): String {
        require(!kNode.isPartition) {
            "The given Node is of partition type"
        }
        val msg = nodeExistWithExplanation(idFor(kNode))
        require(msg == null) {
            "We can only update existing nodes. While this is not a valid node because: $msg"
        }
        kNode.assignParents()
        val lwNode = toLionWeb(kNode)
        // Now, if the parent of this node is null we need to find the real parent from the model repository
        // Otherwise we need to be sure to set the parent anyway
        if (lwNode.parent == null) {
            val parentIdOnServer = lionWebClient.getParentId(lwNode.id!!)
            lwNode.setParentID(parentIdOnServer)
        }
        lionWebClient.storeTree(lwNode)
        return lwNode.id!!
    }

    fun getLionWebNode(nodeID: String, withProxyParent: Boolean = false) : LWNode {
        return lionWebClient.retrieve(nodeID, withProxyParent)
    }

    /**
     * Here node means "non partition node".
     */
    fun getNode(nodeID: String): KNode {
        val lwNode = lionWebClient.retrieve(nodeID)
        val result = nodeConverter.importModelFromLionWeb(lwNode) as KNode

        fun adjustSource(
            result: KNode,
            lwNode: LWNode,
        ) {
            val nodeID = lwNode.id!!
            if (result !is IDLogic) {
                val ancestorsIds = lionWebClient.getAncestorsId(nodeID)
                val sourceId =
                    when (ancestorsIds.size) {
                        0 -> {
                            require(nodeID.startsWith(PARTITION_PREFIX)) {
                                "Expected node without ancestor to have an ID starting with $PARTITION_PREFIX. " +
                                    "It is instead: $nodeID"
                            }
                            nodeID.removePrefix(PARTITION_PREFIX)
                        }

                        1 -> {
                            // the only ancestor is the partition, so this node is the root
                            if (!isIDBasedOnSource(result)) {
                                if (nodeID.endsWith(ROOT_POSTFIX)) {
                                    nodeID.removeSuffix(ROOT_POSTFIX)
                                } else {
                                    nodeID
                                }
                            } else {
                                require(nodeID.startsWith(SOURCE_PREFIX))
                                nodeID.removePrefix(SOURCE_PREFIX)
                            }
                        }

                        else -> {
                            val ancestorsWithSourcePrefix = ancestorsIds.filter { it.startsWith(SOURCE_PREFIX) }
                            if (ancestorsWithSourcePrefix.isEmpty()) {
                                if (isIDBasedOnSource(result)) {
                                    if (nodeID.startsWith(SOURCE_PREFIX)) {
                                        nodeID.removePrefix(SOURCE_PREFIX)
                                    } else {
                                        throw IllegalStateException(
                                            "We are looking for the source containing $result. " +
                                                "The node has ID $nodeID and it has these ancestors: $ancestorsIds. " +
                                                "We cannot figure out the source has none of its ancestors is starting with " +
                                                "$SOURCE_PREFIX",
                                        )
                                    }
                                } else {
                                    throw IllegalStateException(
                                        "We are looking for the source containing $result. " +
                                            "The node has ID $nodeID and it has these ancestors: $ancestorsIds. " +
                                            "We cannot figure out the source has none of its ancestors is starting with " +
                                            "$SOURCE_PREFIX",
                                    )
                                }
                            } else {
                                ancestorsWithSourcePrefix.last().removePrefix(SOURCE_PREFIX)
                            }
                        }
                    }
                result.withSource(LionWebRootSource(sourceId))
            }

            result.children.forEach { child ->
                // here we count on the cache...
                val lwChild = nodeConverter.exportModelToLionWeb(child)
                adjustSource(child, lwChild)
            }
        }
        adjustSource(result, lwNode)

        require(nodeID == idFor(result)) {
            "We were expecting the node $result to have ID $nodeID while it has ID ${idFor(result)}"
        }
        return result
    }

    //
    // Other operations
    //

    /**
     * To be called exactly once, to ensure the Model Repository is initialized.
     * Note that it causes all content of the Model Repository to be lost!
     */
    fun modelRepositoryInit() {
        lionWebClient.modelRepositoryInit()
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

    // TODO: move this to Code Insight Studio
    fun loadSRI(partitionID: String): SRI {
        val nodeId = SRI.sriNodeID(partitionID)
        return if (getPartitionIDs().contains(nodeId)) {
            SRI.fromLionWeb(lionWebClient.retrieve(nodeId), this)
        } else {
            // If it does not exist then it is empty
            SRI(this, partitionID)
        }
    }

    //
    // Private methods
    //

    private fun toLionWeb(kNode: Node): LWNode {
        require(kNode.isPartition || kNode.source != null || kNode is IDLogic) {
            "When exporting to LionWeb, if the Node is not a partition and it does not implement IDLogic, then we " +
                "consider the source of the node to determine its Node ID, so it should have one"
        }
        kNode.assignParents()
        if (!kNode.isPartition) {
            kNode.walkDescendants().forEach { descendant ->
                if (descendant.source == null) {
                    descendant.source = kNode.source
                }
            }
        }
        nodeConverter.clearNodesMapping()
        return nodeConverter.exportModelToLionWeb(kNode, idProvider, considerParent = true)
    }

    private fun toLionWeb(
        kNode: Node,
        containerID: String,
        containmentName: String,
    ): LWNode {
        require(kNode.isPartition || kNode.source != null || kNode is IDLogic) {
            "When exporting to LionWeb, if the Node is not a partition and it does not implement IDLogic, then we " +
                "consider the source of the node to determine its Node ID, so it should have one"
        }
        kNode.assignParents()
        if (!kNode.isPartition) {
            kNode.walkDescendants().forEach { descendant ->
                if (descendant.source == null) {
                    descendant.source = kNode.source
                }
            }
        }
        nodeConverter.clearNodesMapping()
        return nodeConverter.exportModelToLionWeb(
            kNode,
            idProvider,
            considerParent = true,
            NonRootCoordinates(containerID, containmentName),
        )
    }

    private fun isIDBasedOnSource(node: KNode): Boolean {
        return node !is IDLogic && (node.isPartition || sourceBasedNodeTypes.contains(node::class))
    }

    private fun isIIN(node: KNode): Boolean {
        return isIDBasedOnSource(node) || node is IDLogic
    }

    private fun requireIINode(
        kNode: Node,
        description: String,
    ) {
        require(isIIN(kNode)) {
            "$description should be used only for nodes that can calculate their own ID independently from " +
                "their position. Instead we got $kNode (class: ${kNode.javaClass.canonicalName})"
        }
    }

    private fun debugFile(
        relativePath: String,
        text: () -> String,
    ) {
        debugFileHelper(debug, relativePath, text)
    }
}
