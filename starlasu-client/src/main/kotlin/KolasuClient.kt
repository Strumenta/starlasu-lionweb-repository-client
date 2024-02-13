package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.lionweb.LionWebModelConverter
import com.strumenta.kolasu.model.Node
import com.strumenta.lwrepoclient.base.LionWebClient
import io.lionweb.lioncore.java.language.Enumeration

class KolasuClient(val hostname: String = "localhost", val port: Int = 3005, val debug: Boolean = true) {

    private val nodeConverter = LionWebModelConverter()
    private val lionWebClient = LionWebClient(hostname, port, debug = debug)

    fun registerLanguage(kolasuLanguage: KolasuLanguage) {
        val lionwebLanguage = nodeConverter.exportLanguageToLionWeb(kolasuLanguage)
        lionWebClient.registerLanguage(lionwebLanguage)
        kolasuLanguage.enumClasses.forEach { enumClass ->
            val enumeration = lionwebLanguage.elements.filterIsInstance<Enumeration>().find { it.name == enumClass.simpleName }!!
            lionWebClient.registerPrimitiveSerializer(
                enumeration.id!!
            ) { value -> (value as Enum<*>).name }
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
}
