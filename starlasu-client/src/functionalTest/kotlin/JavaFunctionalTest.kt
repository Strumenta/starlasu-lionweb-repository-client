import com.strumenta.javalangmodule.ast.JClassDeclaration
import com.strumenta.javalangmodule.ast.JCompilationUnit
import com.strumenta.javalangmodule.ast.JEntityType
import com.strumenta.javalangmodule.ast.JFieldDecl
import com.strumenta.javalangmodule.ast.JIntType
import com.strumenta.javalangmodule.ast.JIntegerLiteralExpr
import com.strumenta.javalangmodule.ast.JMethodBody
import com.strumenta.javalangmodule.ast.JMethodDeclaration
import com.strumenta.javalangmodule.ast.JMultiplicationExpr
import com.strumenta.javalangmodule.ast.JPackageDeclaration
import com.strumenta.javalangmodule.ast.JParameterDeclaration
import com.strumenta.javalangmodule.ast.JQualifiedName
import com.strumenta.javalangmodule.ast.JReferenceExpr
import com.strumenta.javalangmodule.ast.JReturnStatement
import com.strumenta.javalangmodule.ast.JSumExpr
import com.strumenta.javalangmodule.ast.JVariableDeclarator
import com.strumenta.javalangmodule.ast.JVoidType
import com.strumenta.javalangmodule.ast.kLanguage
import com.strumenta.javalangmodule.parser.JavaKolasuParser
import com.strumenta.kolasu.ids.Coordinates
import com.strumenta.kolasu.ids.IDLogic
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.lionweb.LionWebPartition
import com.strumenta.kolasu.model.CodeBaseSource
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.lwrepoclient.kolasu.KolasuClient
import com.strumenta.lwrepoclient.kolasu.withSource
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.Test
import kotlin.test.assertEquals

@LionWebPartition
data class SimplePartition(
    val name: String,
    val stuff: MutableList<Node> = mutableListOf(),
) : Node(), IDLogic {
    override fun calculatedID(coordinates: Coordinates): String {
        return "SimplePartition_$name"
    }
}

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
    fun canCreateAndRetrievePartition() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort)
        kolasuClient.registerLanguage(simplePartitionLanguage)
        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        val myPartition1 = SimplePartition("Foo")
        val myPartitionID = kolasuClient.createPartition(myPartition1)
        assertEquals("partition_SimplePartition_Foo", myPartitionID)
        assertEquals(myPartitionID, kolasuClient.idFor(myPartition1))

        assertEquals(listOf(myPartitionID), kolasuClient.getPartitionIDs())

        val myPartition2 = kolasuClient.retrievePartition(myPartitionID)
        assertASTsAreEqual(myPartition1, myPartition2)
        assertEquals(myPartitionID, kolasuClient.idFor(myPartition2))
    }

    @Test
    fun canCreateAndDeletePartitionUsingNode() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort)
        kolasuClient.registerLanguage(simplePartitionLanguage)
        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        val myPartition1 = SimplePartition("Foo")
        val myPartitionID = kolasuClient.createPartition(myPartition1)
        assertEquals(listOf(myPartitionID), kolasuClient.getPartitionIDs())

        kolasuClient.deletePartition(myPartition1)
        assertEquals(emptyList(), kolasuClient.getPartitionIDs())
    }

    @Test
    fun canCreateAndDeletePartitionUsingID() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort)
        kolasuClient.registerLanguage(simplePartitionLanguage)
        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        val myPartition1 = SimplePartition("Foo")
        val myPartitionID = kolasuClient.createPartition(myPartition1)
        assertEquals(listOf(myPartitionID), kolasuClient.getPartitionIDs())

        kolasuClient.deletePartition(myPartitionID)
        assertEquals(emptyList(), kolasuClient.getPartitionIDs())
    }

    @Test
    fun storePartitionWithSingleASTAndGetItBack() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(kLanguage)
        kolasuClient.registerLanguage(simplePartitionLanguage)
        kolasuClient.sourceBasedNodeTypes.add(JCompilationUnit::class)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val partition = SimplePartition("Baz")
        val partitionID = kolasuClient.createPartition(partition)

        val partitionIDs = kolasuClient.getPartitionIDs()
        val expectedPartitionId = kolasuClient.idFor(partition)
        assertEquals(expectedPartitionId, partitionID)
        assertEquals(listOf(expectedPartitionId), partitionIDs)

        // Now we want to attach a tree to the existing partition
        val javaAst1 = JavaKolasuParser().parse("""class A {}""").root!!
        javaAst1.withSource(CodeBaseSource("My codebase", "foo/A.java"))
        kolasuClient.createNode(javaAst1, expectedPartitionId, SimplePartition::stuff)

        // I can retrieve the entire partition
        partition.stuff.add(javaAst1)
        partition.assignParents()
        val retrievedPartition = kolasuClient.getNode(expectedPartitionId)
        assertASTsAreEqual(partition, retrievedPartition)

        // I can retrieve just a portion of that partition. In that case the parent of the root of the
        // subtree will appear null
        javaAst1.parent = null
        val expectedJavaAst1Id = kolasuClient.idFor(javaAst1)
        val retrievedAST1 = kolasuClient.getNode(expectedJavaAst1Id)
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
        kolasuClient.sourceBasedNodeTypes.add(JCompilationUnit::class)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val partition = SimplePartition("Baz")
        val partitionID = kolasuClient.createPartition(partition)

        val partitionIDs = kolasuClient.getPartitionIDs()
        assertEquals(listOf("partition_SimplePartition_Baz"), partitionIDs)

        // Now we want to attach several trees to the existing partition
        val javaAst1 = JavaKolasuParser().parse("""class A {}""").root!!
        javaAst1.withSource(CodeBaseSource("My codebase", "foo/A.java"))
        val javaAst1ID = kolasuClient.createNode(javaAst1, partitionID, SimplePartition::stuff)

        val javaAst2 = JavaKolasuParser().parse("""class B {}""").root!!
        javaAst2.withSource(CodeBaseSource("My codebase", "foo/B.java"))
        val javaAst2ID = kolasuClient.createNode(javaAst2, partitionID, SimplePartition::stuff)

        val javaAst3 = JavaKolasuParser().parse("""class C {}""").root!!
        javaAst3.withSource(CodeBaseSource("My codebase", "foo/C.java"))
        val javaAst3ID = kolasuClient.createNode(javaAst3, partitionID, SimplePartition::stuff)

        // I can retrieve the entire partition
        partition.stuff.add(javaAst1)
        partition.stuff.add(javaAst2)
        partition.stuff.add(javaAst3)
        partition.assignParents()
        val retrievedPartition = kolasuClient.getNode(partitionID)
        assertEquals(partition, retrievedPartition)

        // I can retrieve just a portion of that partition. In that case the parent of the root of the
        // subtree will appear null
        javaAst1.parent = null
        val retrievedAST1 = kolasuClient.getNode(javaAst1ID)
        assertEquals(
            javaAst1,
            retrievedAST1,
        )
        assertEquals(null, retrievedAST1.parent)
        assertEquals(javaAst1ID, kolasuClient.idFor(retrievedAST1))

        javaAst2.parent = null
        val retrievedAST2 = kolasuClient.getNode(javaAst2ID)
        assertEquals(
            javaAst2,
            retrievedAST2,
        )
        assertEquals(null, retrievedAST2.parent)
        assertEquals(javaAst2ID, kolasuClient.idFor(retrievedAST2))

        javaAst3.parent = null
        val retrievedAST3 = kolasuClient.getNode(javaAst3ID)
        assertEquals(
            javaAst3,
            retrievedAST3,
        )
        assertEquals(null, retrievedAST3.parent)
        assertEquals(javaAst3ID, kolasuClient.idFor(retrievedAST3))
    }

    @Test
    fun storeASTModifyItStoreItBack() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(kLanguage)
        kolasuClient.registerLanguage(simplePartitionLanguage)
        kolasuClient.sourceBasedNodeTypes.add(JCompilationUnit::class)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val partition = SimplePartition("Baz")
        val partitionID = kolasuClient.createPartition(partition)

        val partitionIDs = kolasuClient.getPartitionIDs()
        assertEquals(listOf("partition_SimplePartition_Baz"), partitionIDs)

        // We parse an AST
        val javaAst1 = JavaKolasuParser().parse("""class A {}""").root!!
        javaAst1.withSource(CodeBaseSource("My codebase", "foo/A.java"))
        val javaAst1ID = kolasuClient.createNode(javaAst1, partitionID, SimplePartition::stuff)

        // We get it back a first time, and we modify it
        val javaAst1retrieved1 = kolasuClient.getNode(javaAst1ID) as JCompilationUnit
        assertASTsAreEqual(
            JCompilationUnit(
                declarations = mutableListOf(JClassDeclaration("A")),
            ),
            javaAst1retrieved1,
        )
        (javaAst1retrieved1.declarations.first() as JClassDeclaration).name = "MyAClass"
        javaAst1retrieved1.packageDeclaration = JPackageDeclaration(JQualifiedName.fromString("a.b.c"))
        assertASTsAreEqual(
            JCompilationUnit(
                packageDeclaration = JPackageDeclaration(JQualifiedName.fromString("a.b.c")),
                declarations = mutableListOf(JClassDeclaration("MyAClass")),
            ),
            javaAst1retrieved1,
        )
        kolasuClient.updateNode(javaAst1retrieved1)

        // We get it back a second time, to check it is what we expect
        val javaAst1retrieved2 = kolasuClient.getNode(javaAst1ID) as JCompilationUnit
        assertASTsAreEqual(
            JCompilationUnit(
                packageDeclaration = JPackageDeclaration(JQualifiedName.fromString("a.b.c")),
                declarations = mutableListOf(JClassDeclaration("MyAClass")),
            ),
            javaAst1retrieved2,
        )
    }

    @Test
    fun getNodesByConcept() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(kLanguage)
        kolasuClient.registerLanguage(simplePartitionLanguage)
        kolasuClient.sourceBasedNodeTypes.add(JCompilationUnit::class)

        // We create an empty partition
        val partition = SimplePartition("FooBar")
        kolasuClient.createPartition(partition)

        // Now we want to attach several trees to the existing partition
        val javaAst1 =
            JavaKolasuParser().parse(
                """class A {
            |  int i;
            |  void foo(int a) { return i * a + 2; }
            |}
                """.trimMargin(),
            ).root!!
        javaAst1.withSource(CodeBaseSource("My codebase", "foo/A.java"))
        kolasuClient.createNode(javaAst1, partition, SimplePartition::stuff)

        val javaAst2 = JavaKolasuParser().parse("""class B extends A {}""").root!!
        javaAst2.withSource(CodeBaseSource("My codebase", "foo/B.java"))
        kolasuClient.createNode(javaAst2, partition, SimplePartition::stuff)

        val javaAst3 = JavaKolasuParser().parse("""class C {}""").root!!
        javaAst3.withSource(CodeBaseSource("My codebase", "foo/C.java"))
        kolasuClient.createNode(javaAst3, partition, SimplePartition::stuff)

        val result = kolasuClient.nodesByConcept()
        assertEquals(
            setOf(
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
                JReturnStatement::class,
                Position::class,
                Point::class,
            ),
            result.keys,
        )

        assertEquals(
            setOf("codebase_My_codebase_relpath_foo-A-java_root_declarations_0_members_1_body_statements_0_value_right"),
            result[JIntegerLiteralExpr::class],
        )
        assertEquals(setOf("partition_SimplePartition_FooBar"), result[SimplePartition::class])
        assertEquals(
            setOf(
                "source_codebase_My_codebase_relpath_foo-A-java",
                "source_codebase_My_codebase_relpath_foo-B-java",
                "source_codebase_My_codebase_relpath_foo-C-java",
            ),
            result[JCompilationUnit::class],
        )
    }
}
