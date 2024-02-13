
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.testing.assertASTsAreEqual
import com.strumenta.lwrepoclient.kolasu.KolasuClient
import java.io.InputStream
import com.strumenta.kolasu.model.Node as KNode
import io.lionweb.lioncore.java.model.Node as LWNode

abstract class AbstractLionWebConversion<R : KNode>(val kolasuLanguage: KolasuLanguage) {

    protected abstract fun parse(inputStream: InputStream): ParsingResult<R>

    protected fun checkSerializationAndDeserialization(
        inputStream: InputStream,
        astChecker: (ast: R) -> Unit = {},
        lwASTChecker: (lwAST: LWNode) -> Unit = {},
        jsonChecker: (json: String) -> Unit = {}
    ) {
        val result = parse(inputStream)
        val ast = result.root ?: throw IllegalStateException()
        astChecker.invoke(ast)

        val client = KolasuClient()
        client.registerLanguage(kolasuLanguage)
        val baseId = "foo"
        val lwAST = client.nodeConverter.exportModelToLionWeb(ast, baseId)
        lwASTChecker.invoke(lwAST)
        val json = client.jsonSerialization.serializeTreeToJsonString(lwAST)
        jsonChecker.invoke(json)
        val lwASTDeserialized = client.jsonSerialization.deserializeToNodes(json).find { it.id == lwAST.id } ?: throw IllegalStateException()
        val astDeserialized = client.nodeConverter.importModelFromLionWeb(lwASTDeserialized)

        assertASTsAreEqual(ast, astDeserialized)
    }
}
