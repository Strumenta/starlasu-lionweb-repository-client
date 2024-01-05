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
import io.lionweb.api.bulk.IBulk
import io.lionweb.api.bulk.lowlevel.IBulkLowlevel
import io.lionweb.api.bulk.lowlevel.IDeleteResponse
import io.lionweb.api.bulk.lowlevel.IIdsResponse
import io.lionweb.api.bulk.lowlevel.IPartitionsResponse
import io.lionweb.api.bulk.lowlevel.IRetrieveResponse
import io.lionweb.api.bulk.lowlevel.IStoreResponse
import io.lionweb.api.bulk.wrapper.BulkLowlevelWrapper
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.serialization.LowLevelJsonSerialization
import io.lionweb.lioncore.java.serialization.data.SerializedChunk

private class MyBulkLowlevel(val hostname: String = "localhost", val port: Int = 3005) : IBulkLowlevel {

    private val client = HttpClient(CIO)
    private val jsonSerialization = JsonSerialization.getStandardSerialization().apply {
        enableDynamicNodes()
    }
    override fun partitions(): IPartitionsResponse {
        TODO("Not yet implemented")
    }

    override fun retrieve(p0: MutableList<String>?, p1: String?): IRetrieveResponse {
        TODO("Not yet implemented")
    }

    override fun store(p0: SerializedChunk?, p1: String?): IStoreResponse {
        TODO("Not yet implemented")
    }

    override fun delete(p0: MutableList<String>?): IDeleteResponse {
        TODO("Not yet implemented")
    }

    override fun ids(p0: String?): IIdsResponse {
        TODO("Not yet implemented")
    }

}

class LionWebClient(val hostname: String = "localhost", val port: Int = 3005) : BulkLowlevelWrapper(MyBulkLowlevel(hostname, port)) {

    private val client = HttpClient(CIO)
    private val jsonSerialization = JsonSerialization.getStandardSerialization().apply {
        enableDynamicNodes()
    }

    fun registerLanguage(language: Language) {
        jsonSerialization.registerLanguage(language)
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
