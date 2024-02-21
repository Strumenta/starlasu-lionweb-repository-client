package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.lionweb.ConstantSourceIdProvider
import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.lionweb.LionWebModelConverter
import com.strumenta.kolasu.lionweb.LionWebNodeIdProvider
import com.strumenta.kolasu.lionweb.LionWebPartition
import com.strumenta.kolasu.lionweb.PrimitiveValueSerialization
import com.strumenta.kolasu.lionweb.SimpleSourceIdProvider
import com.strumenta.kolasu.lionweb.SourceIdProvider
import com.strumenta.kolasu.lionweb.StructuralLionWebNodeIdProvider
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.SyntheticSource
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.model.containingProperty
import com.strumenta.kolasu.model.indexInContainingProperty
import com.strumenta.kolasu.traversing.walk
import com.strumenta.lwrepoclient.base.LionWebClient
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.utils.CommonChecks
import java.io.File
import java.util.IdentityHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class SourceBasedPartitionIdProvider(val sourceIdProvider: SourceIdProvider = SimpleSourceIdProvider()) : LionWebNodeIdProvider {
    override fun id(kNode: Node): String {
        require(kNode.parent == null)
        require(kNode.source != null)
        // TODO update SimpleSourceIdProvider
        if (kNode.source is SyntheticSource) {
            return "synthetic_" + (kNode.source as SyntheticSource).description
        }
        return sourceIdProvider.sourceId(kNode.source)
    }
}

class MapBasedPartitionIdProvider : LionWebNodeIdProvider {
    private val map = IdentityHashMap<Node, String>()

    override fun id(kNode: Node): String {
        return map[kNode] ?: throw IllegalStateException()
    }

    operator fun set(
        partition: KNode,
        partitionId: String,
    ) {
        map[partition] = partitionId
    }
}

// TODO consider if replacing LionWebNodeIdProvider
interface LionWebRepositoryNodeIdProvider : LionWebNodeIdProvider {
    // fun partitionId(node: Node) : String
}

class DefaultLionWebRepositoryNodeIdProvider(var sourceIdProvider: SourceIdProvider = SimpleSourceIdProvider()) :
    LionWebRepositoryNodeIdProvider, LionWebNodeIdProvider {
    protected open fun partitionId(kNode: Node): String {
        require(kNode.parent == null)
        require(kNode.source != null)
        // TODO update SimpleSourceIdProvider
        if (kNode.source is SyntheticSource) {
            return "synthetic_" + (kNode.source as SyntheticSource).description
        }
        return sourceIdProvider.sourceId(kNode.source)
    }

    override fun id(kNode: Node): String {
        if (kNode.isPartition) {
            return partitionId(kNode)
        }
        val id = "${sourceIdProvider.sourceId(kNode.source)}_${kNode.positionalID}"
        if (!CommonChecks.isValidID(id)) {
            throw IllegalStateException("An invalid LionWeb Node ID has been produced")
        }
        return id
    }

    private val KNode.positionalID: String
        get() {
            return if (this.parent == null) {
                "root"
            } else {
                val cp = this.containingProperty()!!
                val postfix = if (cp.multiple) "${cp.name}_${this.indexInContainingProperty()!!}" else cp.name
                "${this.parent!!.positionalID}_$postfix"
            }
        }
}

class OverridableIdProvider(private val kolasuClient: KolasuClient) : LionWebRepositoryNodeIdProvider {
    private val overrides = IdentityHashMap<KNode, String>()

    override fun id(kNode: KNode): String {
        return if (overrides.containsKey(kNode)) {
            overrides[kNode]!!
        } else {
            kolasuClient.baseIdProvider.id(kNode)
        }
    }

    operator fun set(
        kNode: KNode,
        id: String,
    ) {
        overrides[kNode] = id
    }
}

class KolasuClient(val hostname: String = "localhost", val port: Int = 3005, val debug: Boolean = false) {
    /**
     * Exposed for testing purposes
     */
    val nodeConverter = LionWebModelConverter()
    var baseIdProvider: LionWebRepositoryNodeIdProvider = DefaultLionWebRepositoryNodeIdProvider()
    val idProvider = OverridableIdProvider(this)
    private val lionWebClient = LionWebClient(hostname, port, debug = debug)

    /**
     * Exposed for testing purposes
     */
    val jsonSerialization: JsonSerialization
        get() {
            return nodeConverter.prepareJsonSerialization(
                JsonSerialization.getStandardSerialization().apply {
                    enableDynamicNodes()
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
    fun createPartition(kPartition: Node) {
        if (kPartition.children.isNotEmpty()) {
            throw IllegalArgumentException("When creating a partition, please specify a single node")
        }
        val lwPartition = nodeConverter.exportModelToLionWeb(kPartition, idProvider)
        lionWebClient.createPartition(lwPartition)
    }

    /**
     * When this method is called, we specify an explicitly ID for the Node, and it will be remembered.
     */
    fun createPartition(
        kPartition: Node,
        partitionID: String,
    ) {
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

    fun retrieve(nodeId: String): Node {
        val lwNode = lionWebClient.retrieve(nodeId)
        return nodeConverter.importModelFromLionWeb(lwNode)
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
        containerId: String,
        containment: KProperty1<C, out Collection<out E>>,
    ) {
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
    }

//    fun idForPartition(partition: KNode): String {
//        require(partition.isPartition)
//        return baseIdProvider.id(partition)
//    }

//    fun idForNode(kNode: KNode, partitionId: String, containmentName: String, containmentIndex: Int): String {
//        require(!kNode.isPartition)
//        return SubTreeLionWebNodeIdProvider(partitionId, containmentName, containmentIndex).id(kNode)
//    }

    fun idFor(kNode: KNode): String {
        return idProvider.id(kNode)
    }
}

private class SubTreeLionWebNodeIdProvider(
    val containerId: String,
    val containmentName: String,
    val containmentIndex: Int,
) :
    LionWebNodeIdProvider {
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

// TODO move to Kolasu
val KNode.isPartition
    get() = this::class.annotations.any { it is LionWebPartition }
