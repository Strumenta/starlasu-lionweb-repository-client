import com.strumenta.kolasu.model.assignParents
import com.strumenta.lwrepoclient.kolasu.KolasuClient
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
class TodoFunctionalTest  : AbstractFunctionalTest() {

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

        kolasuClient.appendTree(todoProject, containerId = "my-base_root", containment = TodoAccount::projects)

        // I can retrieve the entire partition
        todoAccount.projects.add(todoProject)
        todoAccount.assignParents()
        val retrievedTodoAccount = kolasuClient.retrieve("my-base_root")
        assertEquals(todoAccount, retrievedTodoAccount)

        // I can retrieve just a portion of that partition. In that case the parent of the root of the
        // subtree will appear null
        val retrievedTodoProject = kolasuClient.retrieve("my-base_root_projects_0")
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