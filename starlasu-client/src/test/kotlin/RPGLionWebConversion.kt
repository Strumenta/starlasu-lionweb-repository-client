import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.rpgparser.RPGKolasuParser
import com.strumenta.rpgparser.model.CompilationUnit
import com.strumenta.rpgparser.model.kLanguage
import org.apache.commons.io.input.BOMInputStream
import java.io.InputStream
import kotlin.test.Test

class RPGLionWebConversion : AbstractLionWebConversion<CompilationUnit>(kLanguage) {
    override fun parse(inputStream: InputStream): ParsingResult<CompilationUnit> {
        val parser = RPGKolasuParser()
        RPGKolasuParser.parseComments = false
        return parser.parse(BOMInputStream(inputStream))
    }

    @Test
    fun cus300() {
        val inputStream = this.javaClass.getResourceAsStream("/rpg/qrpglesrc/CUS300.rpgle") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }
}
