package com.strumenta.lwrepoclient.base

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.headers
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.serialization.LowLevelJsonSerialization
import io.lionweb.lioncore.java.serialization.PrimitiveValuesSerialization.PrimitiveSerializer
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8

class LionWebClient(val hostname: String = "localhost", val port: Int = 3005) {

    private val client = HttpClient(CIO){
        install(ContentEncoding) {
            deflate(1.0F)
            gzip(0.9F)
        }
    }
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
        return nodes.find { it.id == rootId } ?: throw IllegalArgumentException()
    }

    suspend fun storeTree(node: Node) {
        checkTree(node, jsonSerialization = jsonSerialization)
        val json = jsonSerialization.serializeTreesToJsonString(node)
        println("  Sending ${json!!.encodeToByteArray().size} bytes")
        File("sent.json").writeText(json)
        //https://docs.oracle.com/javase/8/docs/api/java/util/zip/GZIPOutputStream.html#GZIPOutputStream-java.io.OutputStream-
        val response: HttpResponse = client.post("http://$hostname:$port/bulk/store") {
            setBody(
                TextContent(
                    text = json,
                    contentType = ContentType.Application.Json
                )
            )

        }
        println("  Response: ${response.status}")
        if (response.status.value != 200) {
            println("  Response: ${response.bodyAsText()}")
        }
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


private fun String.gzip() : ByteArray {
//    val os = ByteArrayOutputStream()
//    val gzipOs = GZIPOutputStream(os, true)
//    gzipOs.writer(Charsets.UTF_8).write(this)
//    gzipOs.flush()
//    return os.toByteArray()
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(this) }
    return bos.toByteArray()
}

fun ungzipToString(content: ByteArray): String =
    GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() }
