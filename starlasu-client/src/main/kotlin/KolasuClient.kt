package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.lionweb.ConstantSourceIdProvider
import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.lionweb.LionWebModelConverter
import com.strumenta.kolasu.lionweb.LionWebNodeIdProvider
import com.strumenta.kolasu.lionweb.PrimitiveValueSerialization
import com.strumenta.kolasu.lionweb.SourceIdProvider
import com.strumenta.kolasu.lionweb.StructuralLionWebNodeIdProvider
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.model.containingProperty
import com.strumenta.kolasu.model.indexInContainingProperty
import com.strumenta.lwrepoclient.base.LionWebClient
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.utils.CommonChecks
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class KolasuClient(val hostname: String = "localhost", val port: Int = 3005, val debug: Boolean = false) {
    /**
     * Exposed for testing purposes
     */
    val nodeConverter = LionWebModelConverter()
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

    fun createPartition(
        kNode: Node,
        baseId: String,
    ) {
        if (kNode.children.isNotEmpty()) {
            throw IllegalArgumentException("When creating a partition, please specify a single node")
        }
        val lwNode = nodeConverter.exportModelToLionWeb(kNode, StructuralLionWebNodeIdProvider(baseId))
        lionWebClient.createPartition(lwNode)
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
        if (debug) {
            File("lwTreeToAppend.json").writeText(
                nodeConverter.prepareJsonSerialization().serializeTreesToJsonString(lwTreeToAppend),
            )
        }
        lionWebClient.appendTree(lwTreeToAppend, containerId, containment.name)
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
