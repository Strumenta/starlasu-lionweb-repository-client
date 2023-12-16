package com.strumenta.starlasu.lwrepoclient.kolasuexample

import com.strumenta.kolasu.model.Origin
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.SyntheticSource
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.starlasu.lwrepoclient.KolasuClient
import com.strumenta.starlasu.lwrepoclient.LionWebClient
import com.strumenta.starlasu.lwrepoclient.dynamicNode

private suspend fun retrieveNodes(client: KolasuClient) {
    val partitionIDs = client.getPartitionIDs()
    println("Nodes: $partitionIDs")
    require(partitionIDs.contains("myTodo_root"))

    val root = client.getPartition("myTodo_root")

    val tp = TodoProject("Personal tasks").apply {
        this.todos.add(Todo("Buy milk"))
        this.todos.add(Todo("Write post about LionWeb"))
        this.todos.add(Todo("Close issue #124"))
    }
    tp.assignParents()

    assertASTsAreEqual(tp, root)

}

private suspend fun storeNodes(client: KolasuClient) {
    val tp = TodoProject("Personal tasks").apply {
        this.todos.add(Todo("Buy milk"))
        this.todos.add(Todo("Write post about LionWeb"))
        this.todos.add(Todo("Close issue #124"))
    }
    tp.assignParents()

    client.storeTree(tp, "myTodo")
}

suspend fun main(args: Array<String>) {
    val client = KolasuClient()
    client.registerLanguage(todoLanguage)

    retrieveNodes(client)

    //storeNodes(client)
}