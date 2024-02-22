import com.strumenta.kolasu.lionweb.PrimitiveValueSerialization
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.lwrepoclient.kolasu.KolasuClient
import com.strumenta.rpgparser.RPGFileType
import com.strumenta.rpgparser.RPGKolasuParser
import com.strumenta.rpgparser.model.AlphabeticEditCode
import com.strumenta.rpgparser.model.CompilationUnit
import com.strumenta.rpgparser.model.EditCode
import com.strumenta.rpgparser.model.NumericEditCode
import com.strumenta.rpgparser.model.kLanguage
import org.apache.commons.io.input.BOMInputStream
import java.io.InputStream
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
        checkSerializationAndDeserialization(inputStream)
    }

    override fun initializeClient(kolasuClient: KolasuClient) {
        kolasuClient.registerPrimitiveValueSerialization(
            EditCode::class,
            object : PrimitiveValueSerialization<EditCode> {
                override fun deserialize(serialized: String): EditCode {
                    return when {
                        serialized.startsWith("alphabetic:") -> AlphabeticEditCode(serialized.removePrefix("alphabetic:")[0])
                        serialized.startsWith("numeric:") -> NumericEditCode(serialized.removePrefix("numeric:").toInt())
                        else -> throw IllegalStateException("Invalid serialized value")
                    }
                }

                override fun serialize(value: EditCode): String {
                    return when (value) {
                        is AlphabeticEditCode -> "alphabetic:${value.code}"
                        is NumericEditCode -> "numeric:${value.code}"
                    }
                }
            },
        )
    }
}
