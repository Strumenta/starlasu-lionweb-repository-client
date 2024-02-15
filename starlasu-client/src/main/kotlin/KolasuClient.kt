package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.lionweb.LionWebModelConverter
import com.strumenta.kolasu.model.Node
import com.strumenta.lwrepoclient.base.LionWebClient
import io.lionweb.lioncore.java.language.Enumeration

class KolasuClient(val hostname: String = "localhost", val port: Int = 3005, val debug: Boolean = true) {

    /**
     * Exposed for testing purposes
     */
    val nodeConverter = LionWebModelConverter()
    private val lionWebClient = LionWebClient(hostname, port, debug = debug)

    /**
     * Exposed for testing purposes
     */
    val jsonSerialization = lionWebClient.jsonSerialization

    fun registerLanguage(kolasuLanguage: KolasuLanguage) {
        val lionwebLanguage = nodeConverter.exportLanguageToLionWeb(kolasuLanguage)
        lionWebClient.registerLanguage(lionwebLanguage)
        kolasuLanguage.enumClasses.forEach { enumClass ->
            val enumeration = lionwebLanguage.elements.filterIsInstance<Enumeration>().find { it.name == enumClass.simpleName }!!
            val ec = enumClass
            lionWebClient.registerPrimitiveSerializer(
                enumeration.id!!
            ) { value -> (value as Enum<*>).name }
            val values = ec.members.find { it.name == "values" }!!.call() as Array<Enum<*>>
            lionWebClient.registerPrimitiveDeserializer(
                enumeration.id!!
            ) { serialized ->
                if (serialized == null) {
                    null
                } else {
                    values.find { it.name == serialized }
                        ?: throw RuntimeException("Cannot find enumeration value for $serialized (enum ${enumClass.qualifiedName})")
                }
            }
        }
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
