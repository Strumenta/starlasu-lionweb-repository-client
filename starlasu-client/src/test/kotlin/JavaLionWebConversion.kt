import com.strumenta.javalangmodule.ast.JClassInstanceCreationExpr
import com.strumenta.javalangmodule.ast.JCompilationUnit
import com.strumenta.javalangmodule.ast.JExpressionQualifiedMethodInvocationExpr
import com.strumenta.javalangmodule.ast.JLocalVariableDeclaration
import com.strumenta.javalangmodule.ast.JMethodDeclaration
import com.strumenta.javalangmodule.ast.kLanguage
import com.strumenta.javalangmodule.parser.JavaKolasuParser
import com.strumenta.kolasu.parsing.ParsingResult
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.serialization.LowLevelJsonSerialization
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaLionWebConversion : AbstractLionWebConversion<JCompilationUnit>(kLanguage) {
    override fun parse(inputStream: InputStream): ParsingResult<JCompilationUnit> {
        return JavaKolasuParser().parse(inputStream)
    }

    @Test
    fun arthasSimplified() {
        val inputStream = this.javaClass.getResourceAsStream("/java/Arthas_simple1.java") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(
            inputStream,
            lwASTChecker = { lwAST: Node ->
                val import = lwAST.getChildrenByContainmentName("imports").first()
                assertEquals(listOf("static"), `import`.concept.allProperties().map { it.name })
                assertEquals(false, import.getPropertyValueByName("static"))
            },
            jsonChecker = { json: String ->
                val chunk = LowLevelJsonSerialization().deserializeSerializationBlock(json)
                val classifierInstance = chunk.classifierInstancesByID["foo_root_imports_0"]!!
                assertEquals(1, classifierInstance.properties.size)
                assertEquals("false", classifierInstance.properties.first().value)
            },
        )
    }

    @Test
    fun arthas() {
        val inputStream = this.javaClass.getResourceAsStream("/java/Arthas.java") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream, astChecker = { ast: JCompilationUnit ->
            val initializer =
                (
                    (ast.declarations[0].members[1] as JMethodDeclaration).body!!
                        .statements[0] as JLocalVariableDeclaration
                ).declarators[0].initializer
            val jClassInstanceCreationExpr =
                (
                    (
                        (initializer as JExpressionQualifiedMethodInvocationExpr)
                            .container as JExpressionQualifiedMethodInvocationExpr
                    )
                        .container as JExpressionQualifiedMethodInvocationExpr
                )
                    .container as JClassInstanceCreationExpr
            assertEquals(31, jClassInstanceCreationExpr.position?.start?.line)
            val body = jClassInstanceCreationExpr.body
            assertEquals(null, body)
        })
    }
}
