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

class LionWebClient(val hostname: String = "localhost", val port: Int = 3005) {

    private val client = HttpClient(CIO)
    private val jsonSerialization = JsonSerialization.getStandardSerialization().apply {
        enableDynamicNodes()
    }

    fun registerLanguage(language: Language) {
        jsonSerialization.registerLanguage(language)
    }

    suspend fun getPartitions() : List<Node> {
        val response: HttpResponse = client.get("http://$hostname:$port/bulk/partitions")
        val data = response.bodyAsText()
        println("GOT $data")
        val nodes = jsonSerialization.deserializeToNodes(data)
        return nodes
    }

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