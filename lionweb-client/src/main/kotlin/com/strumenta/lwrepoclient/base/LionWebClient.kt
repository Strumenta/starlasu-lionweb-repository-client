package com.strumenta.lwrepoclient.base

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.impl.DynamicNode
import io.lionweb.lioncore.java.model.impl.ProxyNode
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.serialization.LowLevelJsonSerialization
import io.lionweb.lioncore.java.serialization.UnavailableNodePolicy
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.HttpURLConnection

data class ClassifierKey(val languageKey: String, val classifierKey: String)

class LionWebClient(
    val hostname: String = "localhost",
    val port: Int = 3005,
    val debug: Boolean = false,
    val jsonSerializationProvider: (() -> JsonSerialization)? = null,
) {
    private var httpClient: OkHttpClient = OkHttpClient()

    private fun log(message: String) {
        if (debug) {
            println(message)
        }
    }

    /**
     * Exposed for testing purposes
     */
    val defaultJsonSerialization =
        JsonSerialization.getStandardSerialization().apply {
            enableDynamicNodes()
        }

    val jsonSerialization: JsonSerialization
        get() {
            return jsonSerializationProvider?.invoke() ?: defaultJsonSerialization
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

    fun deletePartition(node: Node) {
        deletePartition(node.id ?: throw IllegalStateException("Node ID not specified"))
    }

    fun deletePartition(nodeID: String) {
        val body: RequestBody = "[\"$nodeID\"]".toRequestBody(JSON)
        val request: Request =
            Request.Builder()
                .url("http://$hostname:$port/bulk/deletePartitions")
                .post(body)
                .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code != HttpURLConnection.HTTP_OK) {
                val body = response.body?.string()
                if (debug) {
                    println("  Response: ${response.code}")
                    println("  Response: $body")
                }
                throw RuntimeException("Request failed with code ${response.code}: $body")
            }
        }
    }

    private fun <T> processChunkResponse(
        data: String,
        chunkProcessor: (JsonElement) -> T,
    ): T {
        val json = JsonParser.parseString(data).asJsonObject
        val success = json.get("success").asBoolean
        val messages = json.get("messages").asJsonArray
        if (!messages.isEmpty) {
            log("Messages received: $messages")
        }
        if (!success) {
            throw RuntimeException("Request failed. Messages: $messages")
        }
        val chunkJson = json.get("chunk")
        return chunkProcessor.invoke(chunkJson)
    }

    fun getPartitionIDs(): List<String> {
        val url = "http://$hostname:$port/bulk/partitions"
        val request: Request =
            Request.Builder()
                .url(url)
                .addHeader("Accept-Encoding", "gzip")
                .get()
                .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code == HttpURLConnection.HTTP_OK) {
                val data =
                    (response.body ?: throw IllegalStateException("Response without body when querying $url")).string()
                return processChunkResponse(data) {
                    val chunk = LowLevelJsonSerialization().deserializeSerializationBlock(it)
                    chunk.classifierInstances.mapNotNull { it.id }
                }
            } else {
                throw RuntimeException("Got back ${response.code}: ${response.body?.string()}")
            }
        }
    }

    fun retrieve(rootId: String): Node {
        require(rootId.isNotBlank())
        val body: RequestBody = "{\"ids\":[\"$rootId\"] }".toRequestBody(JSON)
        val url = "http://$hostname:$port/bulk/retrieve"
        val urlBuilder = url.toHttpUrlOrNull()!!.newBuilder()
        urlBuilder.addQueryParameter("depthLimit", "99")
        val request: Request =
            Request.Builder()
                .url(urlBuilder.build())
                .post(body)
                .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code == HttpURLConnection.HTTP_OK) {
                val data =
                    (response.body ?: throw IllegalStateException("Response without body when querying $url")).string()
                debugFile("retrieved-$rootId.json") { data }
                jsonSerialization.unavailableParentPolicy = UnavailableNodePolicy.NULL_REFERENCES
                jsonSerialization.unavailableReferenceTargetPolicy = UnavailableNodePolicy.PROXY_NODES

                return processChunkResponse(data) {
                    val nodes = jsonSerialization.deserializeToNodes(it)
                    nodes.find { it.id == rootId } ?: throw IllegalArgumentException(
                        "When requesting a subtree with rootId=$rootId we got back an answer without such ID. " +
                            "IDs we got back: ${nodes.map { it.id }.joinToString(", ")}",
                    )
                }
            } else {
                throw RuntimeException(
                    "Something went wrong while querying $url: http code ${response.code}, body: ${response.body?.string()}",
                )
            }
        }
    }

    fun getAncestorsId(nodeID: String): List<String> {
        val result = mutableListOf<String>()
        var currentNodeID: String? = nodeID
        while (currentNodeID != null) {
            currentNodeID = getParentId(currentNodeID)
            if (currentNodeID != null) {
                result.add(currentNodeID)
            }
        }
        return result
    }

    fun isNodeExisting(nodeID: String): Boolean {
        require(nodeID.isNotBlank())
        val body: RequestBody = "{\"ids\":[\"$nodeID\"] }".toRequestBody(JSON)
        val url = "http://$hostname:$port/bulk/retrieve"
        val urlBuilder = url.toHttpUrlOrNull()!!.newBuilder()
        urlBuilder.addQueryParameter("depthLimit", "0")
        val request: Request =
            Request.Builder()
                .url(urlBuilder.build())
                .post(body)
                .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code == HttpURLConnection.HTTP_OK) {
                val data =
                    (response.body ?: throw IllegalStateException("Response without body when querying $url")).string()
                debugFile("isNodeExisting-$nodeID.json") { data }
                return processChunkResponse(data) { chunk ->
                    val nodes = chunk.asJsonObject.get("nodes").asJsonArray
                    !nodes.isEmpty
                }
            } else {
                throw RuntimeException(
                    "Something went wrong while querying $url: http code ${response.code}, body: ${response.body?.string()}",
                )
            }
        }
    }

    fun getParentId(nodeID: String): String? {
        require(nodeID.isNotBlank())
        val body: RequestBody = "{\"ids\":[\"$nodeID\"] }".toRequestBody(JSON)
        val url = "http://$hostname:$port/bulk/retrieve"
        val urlBuilder = url.toHttpUrlOrNull()!!.newBuilder()
        urlBuilder.addQueryParameter("depthLimit", "0")
        val request: Request =
            Request.Builder()
                .url(urlBuilder.build())
                .post(body)
                .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code == HttpURLConnection.HTTP_OK) {
                val data =
                    (response.body ?: throw IllegalStateException("Response without body when querying $url")).string()
                debugFile("getParentId-$nodeID.json") { data }
                return processChunkResponse(data) { chunk ->
                    val nodes = chunk.asJsonObject.get("nodes").asJsonArray
                    require(nodes.size() == 1) {
                        "When asking for the parent Id of $nodeID we were expecting to get one node back. " +
                            "We got ${nodes.size()}"
                    }
                    val node = nodes.get(0).asJsonObject
                    require(nodeID == node.get("id").asString)
                    val parentNode = node.get("parent")
                    if (parentNode.isJsonNull) {
                        null
                    } else {
                        parentNode.asString
                    }
                }
            } else {
                throw RuntimeException(
                    "Something went wrong while querying $url: http code ${response.code}, body: ${response.body?.string()}",
                )
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
        val request: Request =
            Request.Builder()
                .url(url)
                .post("".toRequestBody())
                .build()
        OkHttpClient().newCall(request).execute().use { response ->
            if (response.code != HttpURLConnection.HTTP_OK) {
                throw RuntimeException("DB initialization failed, HTTP ${response.code}: ${response.body?.string()}")
            }
        }
    }

    /**
     * This operation is not atomic. We hope that no one is changing the parent at the very
     * same time.
     */
    fun appendTree(
        treeToAppend: Node,
        containerId: String,
        containmentName: String,
    ) {
        // TODO avoid retrieving the whole parent (just do level 1)
        // 1. Retrieve the parent
        val parent = retrieve(containerId)

        // 2. Add the tree to the parent
        val containment =
            parent.concept.getContainmentByName(containmentName)
                ?: throw IllegalArgumentException("The container has not containment named $containmentName")
        if (!containment.isMultiple) {
            throw IllegalArgumentException("The indicated containment is not multiple")
        }
        parent.addChild(containment, treeToAppend)
        (treeToAppend as DynamicNode).parent = parent

        (parent as DynamicNode).parent = getParentId(parent.id!!)?.let { ProxyNode(it) }

        // 3. Store the parent
        storeTree(parent)
    }

    fun nodesByClassifier(): Map<ClassifierKey, Set<String>> {
        val url = "http://$hostname:$port/inspection/nodesByClassifier"
        val request: Request =
            Request.Builder()
                .url(url)
                .get()
                .build()
        OkHttpClient().newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (response.code != HttpURLConnection.HTTP_OK) {
                throw RuntimeException("DB initialization failed, HTTP ${response.code}: $body")
            }
            val data = JsonParser.parseString(body)
            val result = mutableMapOf<ClassifierKey, Set<String>>()
            data.asJsonArray.map { it.asJsonObject }.forEach { entry ->
                val classifierKey = ClassifierKey(entry["language"].asString, entry["classifier"].asString)
                val ids: Set<String> = entry["ids"].asJsonArray.map { it.asString }.toSet()
                result[classifierKey] = ids
            }
            return result
        }
    }

    private fun treeStoringOperation(
        node: Node,
        operation: String,
    ) {
        if (debug) {
            try {
                treeSanityChecks(node, jsonSerialization = jsonSerialization)
            } catch (e: RuntimeException) {
                throw RuntimeException("Failed to store tree $node", e)
            }
        }
        val json = jsonSerialization.serializeTreesToJsonString(node)
        println("  JSON of ${json!!.encodeToByteArray().size} bytes")
        debugFile("sent.json") { json }

        val body: RequestBody = json.compress()
        println("  ${body.contentLength()} bytes sent")

        // TODO control with flag http or https
        val url = "http://$hostname:$port/bulk/$operation"
        val request: Request =
            Request.Builder()
                .url(url)
                .addHeader("Content-Encoding", "gzip")
                .post(body)
                .build()
        httpClient.newCall(request).execute().use { response ->
            if (response.code != HttpURLConnection.HTTP_OK) {
                val body = response.body?.string()
                if (debug) {
                    println("  Response: ${response.code}")
                    println("  Response: $body")
                }
                throw RequestFailureException(url, json, response.code, body)
            }
        }
    }

    private fun debugFile(
        relativePath: String,
        text: () -> String,
    ) {
        debugFileHelper(debug, relativePath, text)
    }
}

data class RequestFailureException(
    val url: String,
    val uncompressedBody: String?,
    val responseCode: Int,
    val responseBody: String?,
) : RuntimeException("Request to $url failed with code $responseCode: $responseBody")

fun debugFileHelper(
    debug: Boolean,
    relativePath: String,
    text: () -> String,
) {
    if (debug) {
        val debugDir = File("debug")
        if (!debugDir.exists()) {
            debugDir.mkdir()
        }
        val file = File(debugDir, relativePath)
        file.writeText(text.invoke())
    }
}
