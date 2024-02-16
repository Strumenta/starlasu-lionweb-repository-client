import com.strumenta.lwrepoclient.kolasu.KolasuClient
import org.slf4j.LoggerFactory
import org.testcontainers.Testcontainers.exposeHostPorts
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.function.Consumer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


@Testcontainers
class JavaFunctionalTest {
    val DB_CONTAINER_PORT = 5432

    //@Container
    @JvmField
    var db: PostgreSQLContainer<*>? = null

    @JvmField
    var modelRepository : GenericContainer<*>? = null

    @BeforeTest
    fun setup() {
        val network = Network.newNetwork()
        db = PostgreSQLContainer("postgres:16.1")
            .withNetwork(network)
            .withNetworkAliases("mypgdb")
            .withUsername("postgres")
            .withPassword("lionweb")
            .withExposedPorts(DB_CONTAINER_PORT).apply {
                this.logConsumers = listOf(object : Consumer<OutputFrame> {
                    override fun accept(t: OutputFrame) {
                        println("DB: ${t.utf8String.trimEnd()}")
                    }
                })
            }
        db!!.start()
        val dbPort = db!!.firstMappedPort
        exposeHostPorts(dbPort)
        modelRepository = GenericContainer(
            ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", "lionweb-repository-Dockerfile" ))
            .dependsOn(db)
            .withNetwork(network)
            .withEnv("PGHOST", "mypgdb")
            .withEnv("PGPORT", DB_CONTAINER_PORT.toString())
            .withEnv("PGUSER", "postgres")
            .withEnv("PGDB", "lionweb_test")
            .withExposedPorts(3005).apply {
                this.logConsumers = listOf(object : Consumer<OutputFrame> {
                    override fun accept(t: OutputFrame) {
                        println("MODEL REPO: ${t.utf8String.trimEnd()}")
                    }
                })
            }
        modelRepository!!.withCommand()
        modelRepository!!.start()
    }

    @AfterTest
    fun teardown() {
        modelRepository!!.stop()
    }

    @Test
    fun noPartitionsOnNewModelRepository() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort)
        assertEquals(emptyList(), kolasuClient.getPartitionIDs())
    }

}