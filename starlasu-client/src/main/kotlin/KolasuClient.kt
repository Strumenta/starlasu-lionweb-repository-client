package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.lionweb.LionWebModelConverter
import com.strumenta.kolasu.lionweb.PrimitiveValueSerialization
import com.strumenta.kolasu.model.Node
import com.strumenta.lwrepoclient.base.LionWebClient
import io.lionweb.lioncore.java.serialization.JsonSerialization
import kotlin.reflect.KClass

class KolasuClient(val hostname: String = "localhost", val port: Int = 3005, val debug: Boolean = true) {

    /**
     * Exposed for testing purposes
     */
    val nodeConverter = LionWebModelConverter()
    private val lionWebClient = LionWebClient(hostname, port, debug = debug)

    /**
     * Exposed for testing purposes
     */
    val jsonSerialization : JsonSerialization
        get() {
            return nodeConverter.prepareJsonSerialization(JsonSerialization.getStandardSerialization().apply {
                enableDynamicNodes()
            })
        }

    fun registerLanguage(kolasuLanguage: KolasuLanguage) {
        val lionwebLanguage = nodeConverter.exportLanguageToLionWeb(kolasuLanguage)
        lionWebClient.registerLanguage(lionwebLanguage)
    }

    fun <E:Any>registerPrimitiveValueSerialization(kClass: KClass<E>, primitiveValueSerialization: PrimitiveValueSerialization<E>) {
        nodeConverter.registerPrimitiveValueSerialization(kClass, primitiveValueSerialization)
    }

    fun getPartitionIDs(): List<String> {
        return lionWebClient.getPartitionIDs()
    }

    fun storeTree(kNode: Node, baseId: String) {
        val lwNode = nodeConverter.exportModelToLionWeb(kNode, baseId)
        lionWebClient.storeTree(lwNode)
    }

    fun getPartition(nodeId: String): Node {
        val lwNode = lionWebClient.getPartition(nodeId)
        return nodeConverter.importModelFromLionWeb(lwNode)
    }

    /**
     * To be called exactly once, to ensure the Model Repository is initialized.
     * Note that it causes all content of the Model Repository to be lost!
     */
    fun modelRepositoryInit() {
        lionWebClient.modelRepositoryInit()
    }
}
