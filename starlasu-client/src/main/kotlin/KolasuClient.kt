package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.lionweb.LionWebModelConverter
import com.strumenta.kolasu.model.Node
import com.strumenta.lwrepoclient.base.LionWebClient
import io.lionweb.lioncore.java.language.Enumeration

class KolasuClient(val hostname: String = "localhost", val port: Int = 3005) {

    private val nodeConverter = LionWebModelConverter()
    private val lionWebClient = LionWebClient(hostname, port)
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

    suspend fun getPartitionIDs(): List<String> {
        return lionWebClient.getPartitionIDs()
    }

    suspend fun storeTree(kNode: Node, baseId: String) {
        val lwNode = nodeConverter.exportModelToLionWeb(kNode, baseId)
        lionWebClient.storeTree(lwNode)
    }

    suspend fun getPartition(nodeId: String): Node {
        val lwNode = lionWebClient.getPartition(nodeId)
        return nodeConverter.importModelFromLionWeb(lwNode)
    }
}
