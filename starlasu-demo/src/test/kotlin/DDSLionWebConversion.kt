import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.lionweb.LionWebLanguageConverter
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.lwrepoclient.kolasu.KolasuPrimitiveTypesSerializersProviders
import com.strumenta.rpgparser.RPGFileType
import com.strumenta.rpgparser.RPGKolasuParser
import com.strumenta.rpgparser.model.CompilationUnit
import com.strumenta.rpgparser.model.kLanguage
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.serialization.JsonSerialization
import org.apache.commons.io.input.BOMInputStream
import java.io.InputStream
import kotlin.test.Ignore
import kotlin.test.Test

class DDSLionWebConversion : AbstractLionWebConversion<CompilationUnit>(kLanguage) {

    override fun parse(inputStream: InputStream): ParsingResult<CompilationUnit> {
        val parser = RPGKolasuParser(RPGFileType.DDS)
        RPGKolasuParser.parseComments = false
        return parser.parse(BOMInputStream(inputStream))
    }

    @Test
    fun article() {
        val inputStream = this.javaClass.getResourceAsStream("/rpg/qddssrc/ARTICLE.dds") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }

    @Test
    fun calmap() {
        val inputStream = this.javaClass.getResourceAsStream("/rpg/qddssrc/CALMAP.dds") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }

    @Test
    fun calmapl2() {
        val inputStream = this.javaClass.getResourceAsStream("/rpg/qddssrc/CALMAPL2.dds") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }

    @Test
    fun custome1() {
        val inputStream = this.javaClass.getResourceAsStream("/rpg/qddssrc/CUSTOME1.dds") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }

    @Test
    fun customer() {
        val inputStream = this.javaClass.getResourceAsStream("/rpg/qddssrc/CUSTOMER.dds") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }

    @Test
    fun deord() {
        val inputStream = this.javaClass.getResourceAsStream("/rpg/qddssrc/DEORD.dds") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }

    @Test
    fun orders() {
        val inputStream = this.javaClass.getResourceAsStream("/rpg/qddssrc/ORDERS.dds") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }

    @Test
    fun ordsum() {
        val inputStream = this.javaClass.getResourceAsStream("/rpg/qddssrc/ORDSUM.dds") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream)
    }

    @Test
    fun samref() {
        val inputStream = this.javaClass.getResourceAsStream("/rpg/qddssrc/SAMREF.dds") ?: throw IllegalStateException()
        checkSerializationAndDeserialization(inputStream, kolasuPrimitiveTypesSerializersProviders = object : KolasuPrimitiveTypesSerializersProviders {
            override fun providePrimitiveTypesSerializers(
                jsonSerialization: JsonSerialization,
                lwLanguage: Language,
                kLanguage: KolasuLanguage
            ) {

            }
        })
    }

    private val jsonSerializationConfigurator = {jsonSerialization: JsonSerialization, lionwebLanguageConverter: LionWebLanguageConverter ->
        // TODO()
    }
}
