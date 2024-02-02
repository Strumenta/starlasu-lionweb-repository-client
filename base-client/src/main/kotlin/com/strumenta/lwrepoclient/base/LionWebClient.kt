package com.strumenta.lwrepoclient.base

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.serialization.LowLevelJsonSerialization
import io.lionweb.lioncore.java.serialization.PrimitiveValuesSerialization.PrimitiveSerializer

class LionWebClient(val hostname: String = "localhost", val port: Int = 3005) {

    private val client = HttpClient(CIO)
    private val jsonSerialization = JsonSerialization.getStandardSerialization().apply {
        enableDynamicNodes()
    }

    fun registerLanguage(language: Language) {
        jsonSerialization.registerLanguage(language)
    }

    fun registerPrimitiveSerializer(dataTypeID: String, serializer: PrimitiveSerializer<Any>) {
        jsonSerialization.primitiveValuesSerialization.registerSerializer(dataTypeID, serializer)
    }

    suspend fun getPartitionIDs(): List<String> {
        val response: HttpResponse = client.get("http://$hostname:$port/bulk/partitions")
        val data = response.bodyAsText()
        val chunk = LowLevelJsonSerialization().deserializeSerializationBlock(data)
        return chunk.classifierInstances.mapNotNull { it.id }
    }

    suspend fun getPartition(rootId: String): Node {
        val response: HttpResponse = client.post("http://$hostname:$port/bulk/retrieve") {
            parameter("depthLimit", "99")
            setBody(
                TextContent(
                    text = "{\"ids\":[\"$rootId\"] }",
                    contentType = ContentType.Application.Json
                )
            )
        }
        val data = response.bodyAsText()
        val nodes = jsonSerialization.deserializeToNodes(data)
        return nodes.first()
    }

    suspend fun storeTree(node: Node) {
        val json = jsonSerialization.serializeTreesToJsonString(node)
        println("SENDING $json")
        val response: HttpResponse = client.post("http://$hostname:$port/bulk/store") {
            setBody(
                TextContent(
                    text = json,
                    contentType = ContentType.Application.Json
                )
            )
        }
    }
}
