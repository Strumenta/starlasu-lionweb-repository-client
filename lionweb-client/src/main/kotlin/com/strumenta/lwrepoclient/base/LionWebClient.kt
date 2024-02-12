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
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.BufferedSink
import okio.GzipSink
import okio.buffer
import java.io.File
import java.io.IOException


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

    private val JSON: MediaType = "application/json".toMediaType()

    suspend fun storeTree(node: Node) {
        checkTree(node, jsonSerialization = jsonSerialization)
        val json = jsonSerialization.serializeTreesToJsonString(node)
        println("  JSON of ${json!!.encodeToByteArray().size} bytes")
        // File("sent.json").writeText(json)
        val body: RequestBody = forceContentLength(gzip(json.toRequestBody(JSON)))
        println("  ${body.contentLength()} bytes sent")

        val request: Request = Request.Builder()
            .url("http://$hostname:$port/bulk/store")
            .addHeader("Content-Encoding", "gzip")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            println("  Response: ${response.code}")
            if (response.code != 200) {
                println("  Response: ${response.body?.string()}")
            }
        }

//        //https://docs.oracle.com/javase/8/docs/api/java/util/zip/GZIPOutputStream.html#GZIPOutputStream-java.io.OutputStream-
//        val response: HttpResponse = ktorClient.post("http://$hostname:$port/bulk/store") {
//            this.compress()
//            setBody(
//                TextContent(
//                    text = json,
//                    contentType = ContentType.Application.Json
//                )
//            )
//
//        }
//        println("  Response: ${response.status}")
//        if (response.status.value != 200) {
//            println("  Response: ${response.bodyAsText()}")
//        }
    }
}

private fun checkTree(node: Node, parents: MutableMap<String, String?> = mutableMapOf(), jsonSerialization: JsonSerialization) {
    // Users_ftomassetti_repos_kolasu-java-langmodule_build_downloaded-examples_arthas_core_src_main_java_com_taobao_arthas_core_Arthas_java__root_declarations_0_members_0_parameters_0_type_baseType
    try {
        if (parents.containsKey(node.id!!)) {
            throw IllegalStateException("Node with ID ${node.id} has already a parent")
        }
        parents[node.id!!] = node.parent?.id
        node.concept.allContainments().forEach { containment ->
            val childrenInContainment = containment.children.map { it.id }
            require(childrenInContainment.none { it !== null })
            require(childrenInContainment.distinct() == childrenInContainment)
        }
        node.children.forEach {
            checkTree(it, parents, jsonSerialization)
        }
    } catch (t: Throwable) {
        File("error.json").writeText(jsonSerialization.serializeTreesToJsonString(node.root))
        throw RuntimeException(t)
    }
}

private fun gzip(body: RequestBody): RequestBody {
    return object : RequestBody() {
        override fun contentType(): MediaType? {
            return body.contentType()
        }

        override fun contentLength(): Long {
            return -1 // We don't know the compressed length in advance!
        }

        @Throws(IOException::class)
        override fun writeTo(sink: BufferedSink) {
            val gzipSink: BufferedSink = GzipSink(sink).buffer()
            body.writeTo(gzipSink)
            gzipSink.close()
        }
    }
}

@Throws(IOException::class)
private fun forceContentLength(requestBody: RequestBody): RequestBody {
    val buffer: Buffer = Buffer()
    requestBody.writeTo(buffer)
    return object : RequestBody() {
        override fun contentType(): MediaType? {
            return requestBody.contentType()
        }

        override fun contentLength(): Long {
            return buffer.size
        }

        @Throws(IOException::class)
        override fun writeTo(sink: BufferedSink) {
            sink.write(buffer.snapshot())
        }
    }
}