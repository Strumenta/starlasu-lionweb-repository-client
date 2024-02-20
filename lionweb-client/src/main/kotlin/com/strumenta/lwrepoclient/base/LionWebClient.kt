package com.strumenta.lwrepoclient.base

import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.serialization.LowLevelJsonSerialization
import io.lionweb.lioncore.java.serialization.UnknownParentPolicy
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class LionWebClient(
    val hostname: String = "localhost",
    val port: Int = 3005,
    val debug: Boolean = true
) {

    private var httpClient: OkHttpClient = OkHttpClient()

    /**
     * Exposed for testing purposes
     */
    val jsonSerialization = JsonSerialization.getStandardSerialization().apply {
        enableDynamicNodes()
    }

    fun registerLanguage(language: Language) {
        jsonSerialization.registerLanguage(language)
    }

    fun createPartition(node: Node) {
        if (node.children.isNotEmpty()) {
            throw IllegalArgumentException("When creating a partition, please specify a single node")
        }
        treeStoringOperation(node, "createPartitions")
    }

    fun getPartitionIDs(): List<String> {
        val url = "http://$hostname:$port/bulk/partitions"
        val request: Request = Request.Builder()
            .url(url)
            .addHeader("Accept-Encoding", "gzip")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code == 200) {
                val data =
                    (response.body ?: throw IllegalStateException("Response without body when querying $url")).string()
                val chunk = LowLevelJsonSerialization().deserializeSerializationBlock(data)
                return chunk.classifierInstances.mapNotNull { it.id }
            } else {
                throw RuntimeException("Got back ${response.code}: ${response.body?.string()}")
            }
        }
    }

    fun retrieve(rootId: String): Node {
        val body: RequestBody = "{\"ids\":[\"$rootId\"] }".toRequestBody(JSON)
        val url = "http://$hostname:$port/bulk/retrieve"
        val urlBuilder = url.toHttpUrlOrNull()!!.newBuilder()
        urlBuilder.addQueryParameter("depthLimit", "99")
        val request: Request = Request.Builder()
            .url(urlBuilder.build())
            .post(body)
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code == 200) {
                val data =
                    (response.body ?: throw IllegalStateException("Response without body when querying $url")).string()
                if (debug) {
                    File("retrieved.json").writeText(data)
                }
                jsonSerialization.unknownParentPolicy = UnknownParentPolicy.NULL_REFERENCES
                val nodes = jsonSerialization.deserializeToNodes(data)
                return nodes.find { it.id == rootId } ?: throw IllegalArgumentException()
            } else {
                throw RuntimeException("Something went wrong while querying $url: http code ${response.code}, body: ${response.body?.string()}")
            }
        }
    }

    fun storeTree(node: Node) {
        treeStoringOperation(node, "store")
    }

    /**
     * To be called exactly once, to ensure the Model Repository is initialized.
     * Note that it causes all content of the Model Repository to be lost!
     */
    fun modelRepositoryInit() {
        val url = "http://$hostname:$port/init"
        val request: Request = Request.Builder()
            .url(url)
            .post("".toRequestBody())
            .build()
        OkHttpClient().newCall(request).execute().use { response ->
            if (response.code != 200) {
                throw RuntimeException("DB initialization failed, HTTP ${response.code}: ${response.body?.string()}")
            }
        }
    }

    private fun treeStoringOperation(node: Node, operation: String) {
        if (debug) {
            try {
                treeSanityChecks(node, jsonSerialization = jsonSerialization)
            } catch (e: RuntimeException) {
                throw RuntimeException("Failed to store tree $node", e)
            }
        }
        val json = jsonSerialization.serializeTreesToJsonString(node)
        println("  JSON of ${json!!.encodeToByteArray().size} bytes")
        if (debug) {
            File("sent.json").writeText(json)
        }

        val body: RequestBody = json.compress()
        println("  ${body.contentLength()} bytes sent")

        // TODO control with flag http or https
        val request: Request = Request.Builder()
            .url("http://$hostname:$port/bulk/$operation")
            .addHeader("Content-Encoding", "gzip")
            .post(body)
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code != 200) {
                val body = response.body?.string()
                if (debug) {
                    println("  Response: ${response.code}")
                    println("  Response: $body")
                }
                throw RuntimeException("Request failed with code ${response.code}: $body")
            }
        }
    }
}
