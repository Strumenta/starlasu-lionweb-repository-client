import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.SimpleOrigin
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.SyntheticSource
import com.strumenta.kolasu.model.assignParents
import com.strumenta.lwrepoclient.kolasu.KolasuClient
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.Test
import kotlin.test.assertEquals

@Testcontainers
class TodoFunctionalTest : AbstractFunctionalTest() {
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
        // By default the partition IDs are derived from the source
        todoAccount.setSource(SyntheticSource("my-wonderful-partition"))
        kolasuClient.createPartition(todoAccount)

        val expectedPartitionId = kolasuClient.idFor(todoAccount)
        assertEquals("synthetic_my-wonderful-partition", expectedPartitionId)

        val partitionIDs = kolasuClient.getPartitionIDs()
        assertEquals(listOf(expectedPartitionId), partitionIDs)

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

        kolasuClient.appendTree(todoProject, containerId = expectedPartitionId, containment = TodoAccount::projects)

        // I can retrieve the entire partition
        todoAccount.projects.add(todoProject)
        todoAccount.assignParents()
        val retrievedTodoAccount = kolasuClient.retrieve(expectedPartitionId)
        assertEquals(todoAccount, retrievedTodoAccount)

        // I can retrieve just a portion of that partition. In that case the parent of the root of the
        // subtree will appear null
        val expectedProjectId = kolasuClient.idFor(todoProject)
        assertEquals("synthetic_my-wonderful-partition_projects_0", expectedProjectId)
        val retrievedTodoProject = kolasuClient.retrieve(expectedProjectId)
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

private fun KNode.setSource(source: Source) {
    this.origin = SimpleOrigin(Position(Point(1, 0), Point(1, 0), source), null)
}
