package com.strumenta.lwrepoclient.kolasu.demo

import com.strumenta.javalangmodule.parser.JavaKolasuParser
import com.strumenta.kolasu.model.assignParents
import com.strumenta.lwrepoclient.kolasu.KolasuClient
import java.io.File
import com.strumenta.javalangmodule.ast.kLanguage as JavaKolasuLanguage

private fun retrieveNodes(client: KolasuClient) {
    val root =
        client.getPartition(
            "Users_ftomassetti_repos_kolasu-java-langmodule_build_downloaded-examples_arthas_core_src_main_" +
                "java_com_taobao_arthas_core_Arthas_java__root",
        )

    println(root)
}

private fun storeNodes(client: KolasuClient) {
    val tp =
        TodoProject("Personal tasks").apply {
            this.todos.add(Todo("Buy milk"))
            this.todos.add(Todo("Write post about LionWeb"))
            this.todos.add(Todo("Close issue #124"))
        }
    tp.assignParents()

    client.storeTree(tp, "myTodo")
}

private fun explore(
    file: File,
    client: KolasuClient,
) {
    if (file.isDirectory) {
        file.listFiles()?.forEach {
            explore(it, client)
        }
    } else if (file.isFile) {
        if (file.extension == "java") {
            println("Processing ${file.absolutePath}")
            val res = JavaKolasuParser().parse(file)
            client.storeTree(
                res.root!!,
                file.absolutePath.replace('/', '_')
                    .replace('.', '_').removePrefix("_") + "_",
            )
        }
    }
}

fun main(args: Array<String>) {
    val client = KolasuClient()

    client.registerLanguage(JavaKolasuLanguage)
    val dir = File("/Users/ftomassetti/repos/kolasu-java-langmodule/build/downloaded-examples/arthas")
    val file =
        File(
            "/Users/ftomassetti/repos/kolasu-java-langmodule/build/downloaded-examples/arthas/" +
                "testcase/src/main/java/com/alibaba/arthas/Type.java",
        )
    val file2 =
        File(
            "/Users/ftomassetti/repos/kolasu-java-langmodule/build/downloaded-examples/arthas/" +
                "core/src/main/java/com/taobao/arthas/core/Arthas.java",
        )
    val file3 =
        File(
            "/Users/ftomassetti/repos/kolasu-java-langmodule/build/downloaded-examples/arthas/" +
                "core/src/main/java/com/taobao/arthas/core/view/ClassInfoView.java",
        )

    explore(file2, client)
    val partitionIDs = client.getPartitionIDs()
    println("Partitions: $partitionIDs")
//    client.registerLanguage(todoLanguage)
//
    retrieveNodes(client)

    // storeNodes(client)
}
