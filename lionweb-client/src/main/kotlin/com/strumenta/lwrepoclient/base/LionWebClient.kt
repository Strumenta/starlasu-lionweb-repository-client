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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody


class LionWebClient(val hostname: String = "localhost", val port: Int = 3005) {

    @Deprecated("Use okHTTP")
    private val ktorClient = HttpClient(CIO){
    }

    private var client: OkHttpClient = OkHttpClient()
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
        val response: HttpResponse = ktorClient.get("http://$hostname:$port/bulk/partitions")
        val data = response.bodyAsText()
        val chunk = LowLevelJsonSerialization().deserializeSerializationBlock(data)
        return chunk.classifierInstances.mapNotNull { it.id }
    }

    suspend fun getPartition(rootId: String): Node {
        val response: HttpResponse = ktorClient.post("http://$hostname:$port/bulk/retrieve") {
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
        return nodes.find { it.id == rootId } ?: throw IllegalArgumentException()
    }

    suspend fun storeTree(node: Node) {
        // TODO control with flag
        checkTree(node, jsonSerialization = jsonSerialization)
        val json = jsonSerialization.serializeTreesToJsonString(node)
        println("  JSON of ${json!!.encodeToByteArray().size} bytes")
        // TODO control with flag
        // File("sent.json").writeText(json)
        val body: RequestBody = json.compress()
        println("  ${body.contentLength()} bytes sent")

        // TODO control with flag http or https
        val request: Request = Request.Builder()
            .url("http://$hostname:$port/bulk/store")
            .addHeader("Content-Encoding", "gzip")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            // TODO control with flag
            println("  Response: ${response.code}")
            if (response.code != 200) {
                // TODO control with flag
                println("  Response: ${response.body?.string()}")
            }
        }
    }
}


