package com.strumenta.starlasu.lwrepoclient

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.serialization.LowLevelJsonSerialization

class LionWebClient(val hostname: String = "localhost", val port: Int = 3005) {

    private val client = HttpClient(CIO)
    private val jsonSerialization = JsonSerialization.getStandardSerialization().apply {
        enableDynamicNodes()
    }

    fun registerLanguage(language: Language) {
        jsonSerialization.registerLanguage(language)
    }

    suspend fun getPartitionIDs() : List<String> {
        val response: HttpResponse = client.get("http://$hostname:$port/bulk/partitions")
        val data = response.bodyAsText()
        val chunk = LowLevelJsonSerialization().deserializeSerializationBlock(data)
        return chunk.classifierInstances.mapNotNull { it.id }
    }

    suspend fun getPartition(rootId: String) : Node {
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