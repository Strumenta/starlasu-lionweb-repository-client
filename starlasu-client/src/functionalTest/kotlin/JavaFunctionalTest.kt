import com.strumenta.javalangmodule.ast.kLanguage
import com.strumenta.javalangmodule.parser.JavaKolasuParser
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.lionweb.LionWebPartition
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.assignParents
import com.strumenta.lwrepoclient.kolasu.KolasuClient
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.Test
import kotlin.test.assertEquals


@LionWebPartition
data class SimplePartition(val stuff: MutableList<Node> = mutableListOf()) : Node()

@Testcontainers
class JavaFunctionalTest : AbstractFunctionalTest() {

    private val simplePartitionLanguage = KolasuLanguage("my.simple.partition.language").apply {
        addClass(SimplePartition::class)
    }

    @Test
    fun noPartitionsOnNewModelRepository() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort)
        assertEquals(emptyList(), kolasuClient.getPartitionIDs())
    }

    @Test
    fun storePartitionWithSingleASTAndGetItBack() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(kLanguage)
        kolasuClient.registerLanguage(simplePartitionLanguage)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val partition = SimplePartition()
        kolasuClient.createPartition(partition, "myPartition")

        val partitionIDs = kolasuClient.getPartitionIDs()
        assertEquals(listOf("myPartition_root"), partitionIDs)

        // Now we want to attach a tree to the existing partition
        val javaAst1 = JavaKolasuParser().parse("""class A {}""").root!!

        kolasuClient.appendTree(javaAst1, "myPartition_root", SimplePartition::stuff)

        // I can retrieve the entire partition
        partition.stuff.add(javaAst1)
        partition.assignParents()
        val retrievedPartition = kolasuClient.retrieve("myPartition_root")
        assertEquals(partition, retrievedPartition)

        // I can retrieve just a portion of that partition. In that case the parent of the root of the
        // subtree will appear null
        javaAst1.parent = null
        val retrievedAST1 = kolasuClient.retrieve("myPartition_root_stuff_0")
        assertEquals(
            javaAst1,
            retrievedAST1
        )
        assertEquals(null, retrievedAST1.parent)
    }

    @Test
    fun storePartitionWithMultipleASTsAndGetItBack() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(kLanguage)
        kolasuClient.registerLanguage(simplePartitionLanguage)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val partition = SimplePartition()
        kolasuClient.createPartition(partition, "myPartition")

        val partitionIDs = kolasuClient.getPartitionIDs()
        assertEquals(listOf("myPartition_root"), partitionIDs)

        // Now we want to attach several trees to the existing partition
        val javaAst1 = JavaKolasuParser().parse("""class A {}""").root!!
        kolasuClient.appendTree(javaAst1, "myPartition_root", SimplePartition::stuff)

        val javaAst2 = JavaKolasuParser().parse("""class B {}""").root!!
        kolasuClient.appendTree(javaAst2, "myPartition_root", SimplePartition::stuff)

        val javaAst3 = JavaKolasuParser().parse("""class C {}""").root!!
        kolasuClient.appendTree(javaAst3, "myPartition_root", SimplePartition::stuff)

        // I can retrieve the entire partition
        partition.stuff.add(javaAst1)
        partition.stuff.add(javaAst2)
        partition.stuff.add(javaAst3)
        partition.assignParents()
        val retrievedPartition = kolasuClient.retrieve("myPartition_root")
        assertEquals(partition, retrievedPartition)

        // I can retrieve just a portion of that partition. In that case the parent of the root of the
        // subtree will appear null
        javaAst1.parent = null
        val retrievedAST1 = kolasuClient.retrieve("myPartition_root_stuff_0")
        assertEquals(
            javaAst1,
            retrievedAST1
        )
        assertEquals(null, retrievedAST1.parent)

        javaAst2.parent = null
        val retrievedAST2 = kolasuClient.retrieve("myPartition_root_stuff_1")
        assertEquals(
            javaAst2,
            retrievedAST2
        )
        assertEquals(null, retrievedAST2.parent)

        javaAst3.parent = null
        val retrievedAST3 = kolasuClient.retrieve("myPartition_root_stuff_2")
        assertEquals(
            javaAst3,
            retrievedAST3
        )
        assertEquals(null, retrievedAST3.parent)
    }
}
