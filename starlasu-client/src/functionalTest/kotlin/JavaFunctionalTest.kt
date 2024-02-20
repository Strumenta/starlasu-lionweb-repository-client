import com.strumenta.kolasu.model.assignParents
import com.strumenta.lwrepoclient.kolasu.KolasuClient
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

private const val DB_CONTAINER_PORT = 5432

@Testcontainers
class JavaFunctionalTest {
    @JvmField
    var db: PostgreSQLContainer<*>? = null

    @JvmField
    var modelRepository: GenericContainer<*>? = null

    @BeforeTest
    fun setup() {
        val network = Network.newNetwork()
        db =
            PostgreSQLContainer("postgres:16.1")
                .withNetwork(network)
                .withNetworkAliases("mypgdb")
                .withUsername("postgres")
                .withPassword("lionweb")
                .withExposedPorts(DB_CONTAINER_PORT).apply {
                    this.logConsumers =
                        listOf(
                            object : Consumer<OutputFrame> {
                                override fun accept(t: OutputFrame) {
                                    println("DB: ${t.utf8String.trimEnd()}")
                                }
                            },
                        )
                }
        db!!.start()
        val dbPort = db!!.firstMappedPort
        exposeHostPorts(dbPort)
        modelRepository =
            GenericContainer(
                ImageFromDockerfile()
                    .withFileFromClasspath("Dockerfile", "lionweb-repository-Dockerfile"),
            )
                .dependsOn(db)
                .withNetwork(network)
                .withEnv("PGHOST", "mypgdb")
                .withEnv("PGPORT", DB_CONTAINER_PORT.toString())
                .withEnv("PGUSER", "postgres")
                .withEnv("PGDB", "lionweb_test")
                .withExposedPorts(3005).apply {
                    this.logConsumers =
                        listOf(
                            object : Consumer<OutputFrame> {
                                override fun accept(t: OutputFrame) {
                                    println("MODEL REPO: ${t.utf8String.trimEnd()}")
                                }
                            },
                        )
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

    @Test
    fun storePartitionAndGetItBack() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(todoLanguage)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val todoAccount = TodoAccount(mutableListOf())
        kolasuClient.createPartition(todoAccount, "my-base")

        val partitionIDs = kolasuClient.getPartitionIDs()
        assertEquals(listOf("my-base_root"), partitionIDs)

        // Now we want to attach a tree to the existing partition
        val todoProject =
            TodoProject(
                "My errands list",
                mutableListOf(
                    Todo("Buy milk"),
                    Todo("Take the garbage out"),
                    Todo("Go for a walk"),
                ),
            )
        todoProject.assignParents()

        kolasuClient.appendTree(todoProject, "my-base", "my-base_root", TodoAccount::projects)

        // I can retrieve the entire partition
        todoAccount.projects.add(todoProject)
        todoAccount.assignParents()
        val retrievedTodoAccount = kolasuClient.retrieve("my-base_root")
        assertEquals(todoAccount, retrievedTodoAccount)

        // I can retrieve just a portion of that partition. In that case the parent of the root of the
        // subtree will appear null
        val retrievedTodoProject = kolasuClient.retrieve("my-base_my-base_root_projects_0")
        assertEquals(
            TodoProject(
                "My errands list",
                mutableListOf(
                    Todo("Buy milk"),
                    Todo("Take the garbage out"),
                    Todo("Go for a walk"),
                ),
            ).apply { assignParents() },
            retrievedTodoProject,
        )
        assertEquals(null, retrievedTodoProject.parent)
    }
}
