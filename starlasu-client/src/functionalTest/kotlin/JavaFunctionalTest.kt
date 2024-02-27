import com.strumenta.javalangmodule.ast.JClassDeclaration
import com.strumenta.javalangmodule.ast.JCompilationUnit
import com.strumenta.javalangmodule.ast.JEntityType
import com.strumenta.javalangmodule.ast.JFieldDecl
import com.strumenta.javalangmodule.ast.JIntType
import com.strumenta.javalangmodule.ast.JIntegerLiteralExpr
import com.strumenta.javalangmodule.ast.JMethodBody
import com.strumenta.javalangmodule.ast.JMethodDeclaration
import com.strumenta.javalangmodule.ast.JMultiplicationExpr
import com.strumenta.javalangmodule.ast.JParameterDeclaration
import com.strumenta.javalangmodule.ast.JReferenceExpr
import com.strumenta.javalangmodule.ast.JReturnStatement
import com.strumenta.javalangmodule.ast.JSumExpr
import com.strumenta.javalangmodule.ast.JVariableDeclarator
import com.strumenta.javalangmodule.ast.JVoidType
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
    private val simplePartitionLanguage =
        KolasuLanguage("my.simple.partition.language").apply {
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
        kolasuClient.idProvider[partition] = "myPartition"
        kolasuClient.createPartition(partition)

        val partitionIDs = kolasuClient.getPartitionIDs()
        val expectedPartitionId = kolasuClient.idFor(partition)
        assertEquals("myPartition", expectedPartitionId)
        assertEquals(listOf(expectedPartitionId), partitionIDs)

        // Now we want to attach a tree to the existing partition
        val javaAst1 = JavaKolasuParser().parse("""class A {}""").root!!

        kolasuClient.appendTree(javaAst1, expectedPartitionId, SimplePartition::stuff)

        // I can retrieve the entire partition
        partition.stuff.add(javaAst1)
        partition.assignParents()
        val retrievedPartition = kolasuClient.retrieve(expectedPartitionId)
        assertEquals(partition, retrievedPartition)

        // I can retrieve just a portion of that partition. In that case the parent of the root of the
        // subtree will appear null
        javaAst1.parent = null
        val expectedJavaAst1Id = kolasuClient.idFor(javaAst1)
        assertEquals("myPartition", expectedPartitionId)
        val retrievedAST1 = kolasuClient.retrieve(expectedJavaAst1Id)
        assertEquals(
            javaAst1,
            retrievedAST1,
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
        kolasuClient.idProvider[partition] = "myPartition"
        kolasuClient.createPartition(partition)

        val partitionIDs = kolasuClient.getPartitionIDs()
        assertEquals(listOf("myPartition"), partitionIDs)

        // Now we want to attach several trees to the existing partition
        val javaAst1 = JavaKolasuParser().parse("""class A {}""").root!!
        kolasuClient.appendTree(javaAst1, "myPartition", SimplePartition::stuff)

        val javaAst2 = JavaKolasuParser().parse("""class B {}""").root!!
        kolasuClient.appendTree(javaAst2, "myPartition", SimplePartition::stuff)

        val javaAst3 = JavaKolasuParser().parse("""class C {}""").root!!
        kolasuClient.appendTree(javaAst3, "myPartition", SimplePartition::stuff)

        // I can retrieve the entire partition
        partition.stuff.add(javaAst1)
        partition.stuff.add(javaAst2)
        partition.stuff.add(javaAst3)
        partition.assignParents()
        val retrievedPartition = kolasuClient.retrieve("myPartition")
        assertEquals(partition, retrievedPartition)

        // I can retrieve just a portion of that partition. In that case the parent of the root of the
        // subtree will appear null
        javaAst1.parent = null
        val retrievedAST1 = kolasuClient.retrieve("myPartition_stuff_0")
        assertEquals(
            javaAst1,
            retrievedAST1,
        )
        assertEquals(null, retrievedAST1.parent)

        javaAst2.parent = null
        val retrievedAST2 = kolasuClient.retrieve("myPartition_stuff_1")
        assertEquals(
            javaAst2,
            retrievedAST2,
        )
        assertEquals(null, retrievedAST2.parent)

        javaAst3.parent = null
        val retrievedAST3 = kolasuClient.retrieve("myPartition_stuff_2")
        assertEquals(
            javaAst3,
            retrievedAST3,
        )
        assertEquals(null, retrievedAST3.parent)
    }

    @Test
    fun getNodesByConcept() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(kLanguage)
        kolasuClient.registerLanguage(simplePartitionLanguage)

        // We create an empty partition
        val partition = SimplePartition()
        kolasuClient.idProvider[partition] = "myPartition"
        kolasuClient.createPartition(partition)

        // Now we want to attach several trees to the existing partition
        val javaAst1 = JavaKolasuParser().parse("""class A {
            |  int i;
            |  void foo(int a) { return i * a + 2; }
            |}""".trimMargin()).root!!
        kolasuClient.appendTree(javaAst1, "myPartition", SimplePartition::stuff)

        val javaAst2 = JavaKolasuParser().parse("""class B extends A {}""").root!!
        kolasuClient.appendTree(javaAst2, "myPartition", SimplePartition::stuff)

        val javaAst3 = JavaKolasuParser().parse("""class C {}""").root!!
        kolasuClient.appendTree(javaAst3, "myPartition", SimplePartition::stuff)

        val result = kolasuClient.nodesByConcept()
        assertEquals(setOf(
            JIntegerLiteralExpr::class,
            JReferenceExpr::class,
            SimplePartition::class,
            JMethodDeclaration::class,
            JFieldDecl::class,
            JVoidType::class,
            JCompilationUnit::class,
            JParameterDeclaration::class,
            JMultiplicationExpr::class,
            JMethodBody::class,
            JClassDeclaration::class,
            JIntType::class,
            JVariableDeclarator::class,
            JSumExpr::class,
            JEntityType::class,
            JReturnStatement::class
        ), result.keys)

        assertEquals(setOf("myPartition_stuff_0_declarations_0_members_1_body_statements_0_value_right"), result[JIntegerLiteralExpr::class])
        assertEquals(setOf("myPartition"), result[SimplePartition::class])
        assertEquals(setOf("myPartition_stuff_0", "myPartition_stuff_1", "myPartition_stuff_2"), result[JCompilationUnit::class])
    }
}
