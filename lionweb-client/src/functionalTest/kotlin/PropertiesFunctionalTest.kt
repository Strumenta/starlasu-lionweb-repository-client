import com.strumenta.lwrepoclient.base.ClassifierKey
import com.strumenta.lwrepoclient.base.FunctionalTestBuildConfig
import com.strumenta.lwrepoclient.base.LionWebClient
import com.strumenta.lwrepoclient.base.dynamicNode
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
class PropertiesFunctionalTest {
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
                    .withFileFromClasspath("Dockerfile", "lionweb-repository-Dockerfile")
                    .withBuildArg("lionwebRepositoryCommitId", FunctionalTestBuildConfig.LIONWEB_REPOSITORY_COMMIT_ID),
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
        val client = LionWebClient(port = modelRepository!!.firstMappedPort)
        assertEquals(emptyList(), client.getPartitionIDs())
    }

    @Test
    fun isNodeExisting() {
        val client = LionWebClient(port = modelRepository!!.firstMappedPort)
        assertEquals(false, client.isNodeExisting("pp1"))
        assertEquals(false, client.isNodeExisting("pf1"))
        assertEquals(false, client.isNodeExisting("prop1"))
        assertEquals(false, client.isNodeExisting("prop2"))
        assertEquals(false, client.isNodeExisting("prop3"))

        val pp1 = propertiesPartition.dynamicNode("pp1")
        client.createPartition(pp1)

        assertEquals(true, client.isNodeExisting("pp1"))
        assertEquals(false, client.isNodeExisting("pf1"))
        assertEquals(false, client.isNodeExisting("prop1"))
        assertEquals(false, client.isNodeExisting("prop2"))
        assertEquals(false, client.isNodeExisting("prop3"))

        val pf =
            propertiesFile.dynamicNode("pf1").apply {
                parent = pp1
            }
        val prop1 =
            property.dynamicNode("prop1").apply {
                setPropertyValueByName("name", "Prop1")
                pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
            }
        val prop2 =
            property.dynamicNode("prop2").apply {
                setPropertyValueByName("name", "Prop2")
                pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
            }
        val prop3 =
            property.dynamicNode("prop3").apply {
                setPropertyValueByName("name", "Prop3")
                pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
            }
        client.storeTree(pf)

        assertEquals(true, client.isNodeExisting("pp1"))
        assertEquals(true, client.isNodeExisting("pf1"))
        assertEquals(true, client.isNodeExisting("prop1"))
        assertEquals(true, client.isNodeExisting("prop2"))
        assertEquals(true, client.isNodeExisting("prop3"))

        client.deletePartition("pp1")
        assertEquals(false, client.isNodeExisting("pp1"))
        assertEquals(false, client.isNodeExisting("pf1"))
        assertEquals(false, client.isNodeExisting("prop1"))
        assertEquals(false, client.isNodeExisting("prop2"))
        assertEquals(false, client.isNodeExisting("prop3"))
    }

    @Test
    fun gettingPartionsAfterStoringPartitions() {
        val client = LionWebClient(port = modelRepository!!.firstMappedPort)
        client.registerLanguage(propertiesLanguage)

        val pp1 = propertiesPartition.dynamicNode("pp1")
        assertEquals(emptyList(), client.getPartitionIDs())
        client.createPartition(pp1)
        assertEquals(listOf("pp1"), client.getPartitionIDs())
        assertEquals(pp1, client.retrieve("pp1"))
    }

    @Test
    fun gettingNodessAfterStoringNodes() {
        val client = LionWebClient(port = modelRepository!!.firstMappedPort)
        client.registerLanguage(propertiesLanguage)

        val pp1 = propertiesPartition.dynamicNode("pp1")
        client.createPartition(pp1)

        val pf =
            propertiesFile.dynamicNode("pf1").apply {
                parent = pp1
            }
        val prop1 =
            property.dynamicNode("prop1").apply {
                setPropertyValueByName("name", "Prop1")
                pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
            }
        val prop2 =
            property.dynamicNode().apply {
                setPropertyValueByName("name", "Prop2")
                pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
            }
        val prop3 =
            property.dynamicNode().apply {
                setPropertyValueByName("name", "Prop3")
                pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
            }
        client.storeTree(pf)

        val retrieved = client.retrieve("pf1")
        assertEquals(null, retrieved.parent)
        assertEquals("pf1", retrieved.id)
        assertEquals(propertiesFile, retrieved.concept)
    }

    @Test
    fun getNodesByClassifier() {
        val client = LionWebClient(port = modelRepository!!.firstMappedPort)
        client.registerLanguage(propertiesLanguage)

        val pp1 = propertiesPartition.dynamicNode("pp1")
        client.createPartition(pp1)

        val pf =
            propertiesFile.dynamicNode("pf1").apply {
                parent = pp1
            }
        val prop1 =
            property.dynamicNode("prop1").apply {
                setPropertyValueByName("name", "Prop1")
                pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
            }
        val prop2 =
            property.dynamicNode("prop2").apply {
                setPropertyValueByName("name", "Prop2")
                pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
            }
        val prop3 =
            property.dynamicNode("prop3").apply {
                setPropertyValueByName("name", "Prop3")
                pf.addChild(pf.concept.getContainmentByName("properties")!!, this)
            }
        client.storeTree(pf)

        val nodesByClassifier = client.nodesByClassifier()
        assertEquals(
            mapOf(
                ClassifierKey("language-properties-key", "properties-Property-key") to setOf("prop1", "prop2", "prop3"),
                ClassifierKey("language-properties-key", "properties-PropertiesPartition-key") to setOf("pp1"),
                ClassifierKey("language-properties-key", "properties-PropertiesFile-key") to setOf("pf1"),
            ),
            nodesByClassifier,
        )
    }
}
