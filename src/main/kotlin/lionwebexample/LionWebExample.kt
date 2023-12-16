package com.strumenta.starlasu.lwrepoclient.lionwebexample

import com.strumenta.starlasu.lwrepoclient.LionWebClient
import com.strumenta.starlasu.lwrepoclient.dynamicNode

private suspend fun retrieveNodes(client: LionWebClient) {
    val partitionIDs = client.getPartitionIDs()
    println("Nodes: $partitionIDs")

    val tree = client.getPartition(partitionIDs.first())
    require(tree.id =="pf1")
    require(tree.concept.name == "PropertiesFile")
    require(tree.children.size == 1)
    val child = tree.children.first()
    require(child.id =="prop1")
    require(child.concept.name == "Property")
    require(child.children.size == 0)
    require(child.getPropertyValueByName("name") == "Prop1")
}

private suspend fun storeNodes(client: LionWebClient) {
    val pf = propertiesFile.dynamicNode("pf1")
    val prop1 = property.dynamicNode("prop1").apply {
        setPropertyValueByName("name", "Prop1")
        pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
    }
    val prop2 = property.dynamicNode().apply {
        setPropertyValueByName("name", "Prop2")
        pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
    }
    val prop3 = property.dynamicNode().apply {
        setPropertyValueByName("name", "Prop3")
        pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
    }

    client.storeTree(pf)
}

suspend fun main(args: Array<String>) {
    val client = LionWebClient()
    client.registerLanguage(propertiesLanguage)

    retrieveNodes(client)

    // storeNodes(client)
}