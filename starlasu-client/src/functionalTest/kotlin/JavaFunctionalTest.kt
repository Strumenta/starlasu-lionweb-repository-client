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
import com.strumenta.kolasu.lionweb.LWNode
import com.strumenta.kolasu.model.CodeBaseSource
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.lwrepoclient.kolasu.KolasuClient
import com.strumenta.lwrepoclient.kolasu.withSource
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.language.LionCoreBuiltins
import io.lionweb.lioncore.java.language.Property
import io.lionweb.lioncore.java.model.impl.DynamicNode
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.Test
import kotlin.test.assertEquals

val simpleLanguage: Language by lazy {
    val l =
        Language("MyLanguage", "MyLanguageId", "MyLanguageKey").apply {
            this.version = "1"
        }
    l.addElement(
        Concept(
            l,
            "SimplePartition",
            "SimplePartitionId",
            "SimplePartitionKey",
        ).apply {
            this.addFeature(
                Property.createRequired("name", LionCoreBuiltins.getString()).apply {
                    id = "SimplePartition-Name-Id"
                    key = "SimplePartition-Name-Key"
                },
            )
            this.addFeature(
                Containment.createRequired("stuff", LionCoreBuiltins.getNode()).apply {
                    id = "SimplePartition-Stuff-Id"
                    key = "SimplePartition-Stuff-Key"
                    isMultiple = true
                },
            )
        },
    )
    l
}

val simplePartitionConcept by lazy {
    simpleLanguage.getConceptByName("SimplePartition")
}

data class SimplePartition(
    val name: String,
    val stuff: MutableList<LWNode> = mutableListOf(),
) : DynamicNode("partition_SimplePartition_$name", simplePartitionConcept!!) {
    override fun getChildren(containment: Containment): MutableList<io.lionweb.lioncore.java.model.Node> {
        if (containment.name == "stuff") {
            return stuff
        } else {
            throw IllegalArgumentException()
        }
    }
}

@Testcontainers
class JavaFunctionalTest : AbstractFunctionalTest() {
    @Test
    fun noPartitionsOnNewModelRepository() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort)
        assertEquals(emptyList(), kolasuClient.getPartitionIDs())
    }

    @Test
    fun canCreateAndRetrievePartition() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort)
        kolasuClient.registerLanguage(simpleLanguage)
        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        val myPartition1 = SimplePartition("Foo")
        val myPartitionID = kolasuClient.createPartition(myPartition1)
        assertEquals("partition_SimplePartition_Foo", myPartitionID)

        assertEquals(listOf(myPartitionID), kolasuClient.getPartitionIDs())

        val myPartition2 = kolasuClient.retrievePartition(myPartitionID)
        assertLWTreesAreEqual(myPartition1, myPartition2)
    }

    @Test
    fun canCreateAndDeletePartitionUsingNode() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort)
        kolasuClient.registerLanguage(simpleLanguage)
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
        kolasuClient.registerLanguage(simpleLanguage)
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
        kolasuClient.registerLanguage(simpleLanguage)
        kolasuClient.registerLanguage(kLanguage)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val partition = SimplePartition("Baz")
        val partitionID = kolasuClient.createPartition(partition)

        val partitionIDs = kolasuClient.getPartitionIDs()
        assertEquals(listOf(partitionID), partitionIDs)

        // Now we want to attach a tree to the existing partition
        val javaAst1 = JavaKolasuParser().parse("""class A {}""").root!!
        javaAst1.withSource(CodeBaseSource("My codebase", "foo/A.java"))
        val ast1ID = kolasuClient.attachAST(javaAst1, partitionID, SimplePartition::stuff, 0)

        // I can retrieve the entire partition
        // javaAst1.assignParents()
        // partition.stuff.add(kolasuClient.toLionWeb(javaAst1))
        val retrievedPartition = kolasuClient.getLionWebNode(partitionID)
        assertEquals(1, retrievedPartition.getChildrenByContainmentName("stuff").size)
        assertEquals(ast1ID, retrievedPartition.getChildrenByContainmentName("stuff")[0].id)
//        assertLWTreesAreEqual(partition, retrievedPartition)

        // I can retrieve just a portion of that partition. In that case the parent of the root of the
        // subtree will appear null
        javaAst1.parent = null
        val expectedJavaAst1Id = kolasuClient.idFor(javaAst1)
        val retrievedAST1 = kolasuClient.getAST(expectedJavaAst1Id)
        assertEquals(
            javaAst1,
            retrievedAST1,
        )
        assertEquals(null, retrievedAST1.parent)
    }

    @Test
    fun storePartitionWithMultipleASTsAndGetItBack() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(simpleLanguage)
        kolasuClient.registerLanguage(kLanguage)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val partition = SimplePartition("Baz")
        val partitionID = kolasuClient.createPartition(partition)

        val partitionIDs = kolasuClient.getPartitionIDs()
        assertEquals(listOf("partition_SimplePartition_Baz"), partitionIDs)

        // Now we want to attach several trees to the existing partition
        val javaAst1 = JavaKolasuParser().parse("""class A {}""").root!!
        javaAst1.withSource(CodeBaseSource("My codebase", "foo/A.java"))
        val javaAst1ID = kolasuClient.attachAST(javaAst1, partitionID, SimplePartition::stuff, 0)

        val javaAst2 = JavaKolasuParser().parse("""class B {}""").root!!
        javaAst2.withSource(CodeBaseSource("My codebase", "foo/B.java"))
        val javaAst2ID = kolasuClient.attachAST(javaAst2, partitionID, SimplePartition::stuff, 1)

        val javaAst3 = JavaKolasuParser().parse("""class C {}""").root!!
        javaAst3.withSource(CodeBaseSource("My codebase", "foo/C.java"))
        val javaAst3ID = kolasuClient.attachAST(javaAst3, partitionID, SimplePartition::stuff, 2)

        // I can retrieve the entire partition
//        partition.stuff.add(kolasuClient.toLionWeb(javaAst1))
//        partition.stuff.add(kolasuClient.toLionWeb(javaAst2))
//        partition.stuff.add(kolasuClient.toLionWeb(javaAst3))
//        javaAst1.assignParents()
//        javaAst2.assignParents()
//        javaAst3.assignParents()
        val retrievedPartition = kolasuClient.getLionWebNode(partitionID)
        // assertEquals(partition, retrievedPartition)
        assertEquals(3, retrievedPartition.getChildrenByContainmentName("stuff").size)
        assertEquals(javaAst1ID, retrievedPartition.getChildrenByContainmentName("stuff")[0].id)
        assertEquals(javaAst2ID, retrievedPartition.getChildrenByContainmentName("stuff")[1].id)
        assertEquals(javaAst3ID, retrievedPartition.getChildrenByContainmentName("stuff")[2].id)

        // I can retrieve just a portion of that partition. In that case the parent of the root of the
        // subtree will appear null
        javaAst1.parent = null
        val retrievedAST1 = kolasuClient.getAST(javaAst1ID)
        assertEquals(
            javaAst1,
            retrievedAST1,
        )
        assertEquals(null, retrievedAST1.parent)
        assertEquals(javaAst1ID, kolasuClient.idFor(retrievedAST1))

        javaAst2.parent = null
        val retrievedAST2 = kolasuClient.getAST(javaAst2ID)
        assertEquals(
            javaAst2,
            retrievedAST2,
        )
        assertEquals(null, retrievedAST2.parent)
        assertEquals(javaAst2ID, kolasuClient.idFor(retrievedAST2))

        javaAst3.parent = null
        val retrievedAST3 = kolasuClient.getAST(javaAst3ID)
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
        kolasuClient.registerLanguage(simpleLanguage)
        kolasuClient.registerLanguage(kLanguage)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val partition = SimplePartition("Baz")
        val partitionID = kolasuClient.createPartition(partition)

        val partitionIDs = kolasuClient.getPartitionIDs()
        assertEquals(listOf("partition_SimplePartition_Baz"), partitionIDs)

        // We parse an AST
        val javaAst1 = JavaKolasuParser().parse("""class A {}""").root!!
        javaAst1.withSource(CodeBaseSource("My codebase", "foo/A.java"))
        val javaAst1ID = kolasuClient.attachAST(javaAst1, partitionID, SimplePartition::stuff, 0)

        // We get it back a first time, and we modify it
        val javaAst1retrieved1 = kolasuClient.getAST(javaAst1ID) as JCompilationUnit
        assertASTsAreEqual(
            JCompilationUnit(
                declarations = mutableListOf(JClassDeclaration("A")),
            ),
            javaAst1retrieved1,
        )
        assertEquals(javaAst1ID, kolasuClient.idFor(javaAst1retrieved1))
        (javaAst1retrieved1.declarations.first() as JClassDeclaration).name = "MyAClass"
        javaAst1retrieved1.packageDeclaration = JPackageDeclaration(JQualifiedName.fromString("a.b.c"))
        assertASTsAreEqual(
            JCompilationUnit(
                packageDeclaration = JPackageDeclaration(JQualifiedName.fromString("a.b.c")),
                declarations = mutableListOf(JClassDeclaration("MyAClass")),
            ),
            javaAst1retrieved1,
        )
        kolasuClient.updateAST(javaAst1retrieved1)

        // We get it back a second time, to check it is what we expect
        val javaAst1retrieved2 = kolasuClient.getAST(javaAst1ID) as JCompilationUnit
        assertEquals(javaAst1ID, kolasuClient.idFor(javaAst1retrieved2))
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
        kolasuClient.registerLanguage(simpleLanguage)
        kolasuClient.registerLanguage(kLanguage)

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
        kolasuClient.attachAST(javaAst1, partition, SimplePartition::stuff)

        val javaAst2 = JavaKolasuParser().parse("""class B extends A {}""").root!!
        javaAst2.withSource(CodeBaseSource("My codebase", "foo/B.java"))
        kolasuClient.attachAST(javaAst2, partition, SimplePartition::stuff)

        val javaAst3 = JavaKolasuParser().parse("""class C {}""").root!!
        javaAst3.withSource(CodeBaseSource("My codebase", "foo/C.java"))
        kolasuClient.attachAST(javaAst3, partition, SimplePartition::stuff)

        val result = kolasuClient.nodesByConcept()
        assertEquals(
            setOf(
                JIntegerLiteralExpr::class,
                JReferenceExpr::class,
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
            ),
            result.keys,
        )

        assertEquals(
            setOf("codebase_My_codebase_relpath_foo-A-java_declarations_members_1_body_statements_value_right"),
            result[JIntegerLiteralExpr::class]!!.ids,
        )
        assertEquals(
            setOf(
                "codebase_My_codebase_relpath_foo-A-java",
                "codebase_My_codebase_relpath_foo-B-java",
                "codebase_My_codebase_relpath_foo-C-java",
            ),
            result[JCompilationUnit::class]!!.ids,
        )
    }
}
