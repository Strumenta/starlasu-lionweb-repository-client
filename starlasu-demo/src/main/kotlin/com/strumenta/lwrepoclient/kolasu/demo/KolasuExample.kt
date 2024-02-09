package com.strumenta.lwrepoclient.kolasu.demo

import com.strumenta.javalangmodule.ast.kLanguage as JavaKolasuLanguage
import com.strumenta.javalangmodule.parser.JavaKolasuParser
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.lwrepoclient.kolasu.KolasuClient
import java.io.File

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

private suspend fun explore(file: File, client: KolasuClient) {
    if (file.isDirectory) {
        file.listFiles()?.forEach {
            explore(it, client)
        }
    } else if (file.isFile) {
        if (file.extension == "java") {
            println("Processing ${file.absolutePath}")
            val res = JavaKolasuParser().parse(file)
            client.storeTree(res.root!!, file.absolutePath.replace('/', '_').replace('.', '_').removePrefix("_") + "_")
        }
    }
}

suspend fun main(args: Array<String>) {
    val client = KolasuClient()
    client.registerLanguage(JavaKolasuLanguage)
    val dir = File("/Users/ftomassetti/repos/kolasu-java-langmodule/build/downloaded-examples/arthas")
    val file = File("/Users/ftomassetti/repos/kolasu-java-langmodule/build/downloaded-examples/arthas/testcase/src/main/java/com/alibaba/arthas/Type.java")
    val file2 = File("/Users/ftomassetti/repos/kolasu-java-langmodule/build/downloaded-examples/arthas/core/src/main/java/com/taobao/arthas/core/Arthas.java")
    val file3 = File("/Users/ftomassetti/repos/kolasu-java-langmodule/build/downloaded-examples/arthas/core/src/main/java/com/taobao/arthas/core/view/ClassInfoView.java")

   explore(dir, client)
//    client.registerLanguage(todoLanguage)
//
    //retrieveNodes(client)

    // storeNodes(client)
}
