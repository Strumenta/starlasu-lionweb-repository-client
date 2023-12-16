package com.strumenta.starlasu.lwrepoclient

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.impl.DynamicNode
import io.lionweb.lioncore.java.serialization.JsonSerialization
import io.lionweb.lioncore.java.serialization.LowLevelJsonSerialization
import io.lionweb.lioncore.java.serialization.data.SerializedChunk

class LionWebClient(val hostname: String = "localhost", val port: Int = 3005) {

    private val client = HttpClient(CIO)
    private val jsonSerialization = JsonSerialization.getStandardSerialization().apply {
        enableDynamicNodes()
    }

    fun registerLanguage(language: Language) {
        jsonSerialization.registerLanguage(language)
    }

    suspend fun getPartitions() : List<String> {
        val response: HttpResponse = client.get("http://$hostname:$port/bulk/partitions")
        val data = response.bodyAsText()
        val chunk = LowLevelJsonSerialization().deserializeSerializationBlock(data)
        return chunk.classifierInstances.mapNotNull { it.id }
    }

//    suspend fun getTree(rootId: String) : Node {
//        val response: HttpResponse = client.post("http://$hostname:$port/getNodeTree") {
//            setBody(TextContent(
//                text = "{\"ids\":[\"$rootId\"]}",
//                contentType = ContentType.Application.Json
//            ))
//        }
//        val data = response.bodyAsText()
//        val nodes = jsonSerialization.deserializeToNodes(data)
//        return nodes.first()
//    }

    suspend fun getTree(rootId: String) : Node {
        val response: HttpResponse = client.post("http://$hostname:$port/bulk/retrieve") {
            parameter("depthLimit", "99")
            setBody(TextContent(
                text = "{\"ids\":[\"$rootId\"] }",
                contentType = ContentType.Application.Json
            ))
        }
        val data = response.bodyAsText()
        val nodes = jsonSerialization.deserializeToNodes(data)
        return nodes.first()
    }


//    suspend fun getPartitions() : List<Node> {
//        val response: HttpResponse = client.get("http://$hostname:$port/bulk/partitions")
//        val data = response.bodyAsText()
//        println("GOT $data")
//        val nodes = jsonSerialization.deserializeToNodes(data)
//        return nodes
//    }

    suspend fun storeTree(node: Node) {
        val json = jsonSerialization.serializeTreesToJsonString(node)
        println("SENDING $json")
        val response: HttpResponse = client.post("http://$hostname:$port/bulk/store") {
            setBody(TextContent(
                text = json,
                contentType = ContentType.Application.Json
            ))
        }
    }
}

suspend fun main(args: Array<String>) {
    val client = LionWebClient()
    client.registerLanguage(propertiesLanguage)
    val nodes = client.getPartitions()
    println("Nodes: $nodes")

    val tree = client.getTree(nodes.first())
    require(tree.id =="pf1")
    require(tree.concept.name == "PropertiesFile")
    require(tree.children.size == 1)
    val child = tree.children.first()
    //require(child.containmentFeature.name == "properties")
    require(child.id =="prop1")
    require(child.concept.name == "Property")
    require(child.children.size == 0)
    require(child.getPropertyValueByName("name") == "Prop1")

    val pf = propertiesFile.dynamicNode("pf1")
    val prop1 = property.dynamicNode("prop1").apply {
        setPropertyValueByName("name", "Prop1")
        pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
    }
//    val prop2 = property.dynamicNode().apply {
//        setPropertyValueByName("name", "Prop2")
//        pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
//    }
//    val prop3 = property.dynamicNode().apply {
//        setPropertyValueByName("name", "Prop3")
//        pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
//    }

    //client.storeTree(pf)

}