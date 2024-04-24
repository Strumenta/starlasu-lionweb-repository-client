import com.strumenta.egl.ast.EglCompilationUnit
import com.strumenta.egl.ast.kLanguage
import com.strumenta.egl.parser.EGLKolasuParser
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.lwrepoclient.kolasu.KolasuClient
import java.io.InputStream
import kotlin.test.Test

class EGLLionWebConversion : AbstractLionWebConversion<EglCompilationUnit>(kLanguage) {
    override fun parse(inputStream: InputStream): ParsingResult<EglCompilationUnit> {
        val parser = EGLKolasuParser()
        return parser.parse(inputStream)
    }

    @Test
    fun sqlBatchSimplified() {
        val inputStream = this.javaClass.getResourceAsStream("/egl/SQLBatch_simplified.egl") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }

    @Test
    fun sqlBatch() {
        val inputStream = this.javaClass.getResourceAsStream("/egl/SQLBatch.egl") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }

    @Test
    fun recordConcept() {
        val client = KolasuClient()
        client.registerLanguage(kolasuLanguage)
        initializeClient(client)
        val recordConcept = client.nodeConverter.correspondingLanguage(kolasuLanguage).getConceptByName("Record")!!
        recordConcept.implemented.size
        recordConcept.getFeatureByName("name")!!
    }

    @Test
    fun `rosetta-code-count-examples-1`() {
        val inputStream = this.javaClass.getResourceAsStream("/egl/rosetta-code-count-examples-1.egl") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }

    @Test
    fun `rosetta-code-count-examples-2`() {
        val inputStream = this.javaClass.getResourceAsStream("/egl/rosetta-code-count-examples-2.egl") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }
}
