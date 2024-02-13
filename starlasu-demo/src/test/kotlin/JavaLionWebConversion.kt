import com.strumenta.javalangmodule.ast.kLanguage
import com.strumenta.javalangmodule.parser.JavaKolasuParser
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.lwrepoclient.kolasu.KolasuClient
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.serialization.LowLevelJsonSerialization
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaLionWebConversion {

    private fun checkSerializationAndDeserialization(inputStream: InputStream,
                                                     lwASTChecker: (lwAST:Node)->Unit = {},
                                                     jsonChecker: (json:String)->Unit = {}) {
        val result = JavaKolasuParser().parse(inputStream)
        val ast = result.root ?: throw IllegalStateException()

        val client = KolasuClient()
        client.registerLanguage(kLanguage)
        val baseId = "foo"
        val lwAST = client.nodeConverter.exportModelToLionWeb(ast, baseId)
        lwASTChecker.invoke(lwAST)
        val json = client.jsonSerialization.serializeTreeToJsonString(lwAST)
        jsonChecker.invoke(json)
        val lwASTDeserialized = client.jsonSerialization.deserializeToNodes(json).find { it.id == lwAST.id } ?: throw IllegalStateException()
        val astDeserialized = client.nodeConverter.importModelFromLionWeb(lwASTDeserialized)

        assertASTsAreEqual(ast, astDeserialized)
    }

    @Test
    fun arthasSimplified() {
        val inputStream = this.javaClass.getResourceAsStream("/Arthas_simple1.java") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream,
            lwASTChecker = {lwAST: Node ->
                           val import = lwAST.getChildrenByContainmentName("imports").first()
                        assertEquals(listOf("static"), `import`.concept.allProperties().map { it.name })
                            assertEquals(false, import.getPropertyValueByName("static"))
            },
            jsonChecker = {json: String ->
            val chunk = LowLevelJsonSerialization().deserializeSerializationBlock(json)
            val classifierInstance = chunk.classifierInstancesByID["foo_root_imports_0"]!!
            assertEquals(1, classifierInstance.properties.size)
            assertEquals("false", classifierInstance.properties.first().value)
        })
    }

    @Test
    fun arthas() {
        val inputStream = this.javaClass.getResourceAsStream("/Arthas.java") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }

}