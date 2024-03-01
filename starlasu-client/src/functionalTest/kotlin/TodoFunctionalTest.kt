import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.lionweb.LionWebLanguageConverter
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.SimpleOrigin
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.SyntheticSource
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository
import com.strumenta.lwrepoclient.kolasu.KolasuClient
import io.lionweb.lioncore.java.serialization.JsonSerialization
import junit.framework.TestCase.assertTrue
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
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

    @Test
    fun checkNodeIDs() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(todoLanguage)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val todoAccount = TodoAccount(mutableListOf())
        // By default the partition IDs are derived from the source
        todoAccount.setSource(SyntheticSource("my-wonderful-partition"))
        kolasuClient.createPartition(todoAccount)

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
        kolasuClient.appendTree(todoProject, todoAccount, containment = TodoAccount::projects)

        assertEquals("synthetic_my-wonderful-partition", kolasuClient.idFor(todoAccount))
        assertEquals("synthetic_my-wonderful-partition_projects_0", kolasuClient.idFor(todoProject))
        assertEquals("synthetic_my-wonderful-partition_projects_0_todos_0", kolasuClient.idFor(todoProject.todos[0]))
        assertEquals("synthetic_my-wonderful-partition_projects_0_todos_1", kolasuClient.idFor(todoProject.todos[1]))
        assertEquals("synthetic_my-wonderful-partition_projects_0_todos_2", kolasuClient.idFor(todoProject.todos[2]))
    }

    @Test
    fun symbolResolution() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(todoLanguage)

        // We create an empty partition
        val todoAccount = TodoAccount(mutableListOf())
        todoAccount.setSource(SyntheticSource("my-wonderful-partition"))
        val partitionID = kolasuClient.createPartition(todoAccount)

        // Now we want to attach a tree to the existing partition
        val todoProject1 =
            TodoProject(
                "My errands list",
                mutableListOf(
                    Todo("Buy milk"),
                    Todo("garbage-out", "Take the garbage out"),
                    Todo("Go for a walk"),
                ),
            )
        todoProject1.assignParents()
        kolasuClient.appendTree(todoProject1, todoAccount, containment = TodoAccount::projects)

        var todoProject2 =
            TodoProject(
                "My other errands list",
                mutableListOf(
                    Todo("BD", "Buy diary"),
                    Todo("WD", "Write in diary", ReferenceByName("BD")),
                    Todo("garbage-in", "Produce more garbage", ReferenceByName("garbage-out")),
                ),
            )
        todoProject2.assignParents()
        val todoProject2ID = kolasuClient.appendTree(todoProject2, todoAccount, containment = TodoAccount::projects)

        var sri = kolasuClient.loadSRI(partitionID)
        var todosInSri = sri.find(Todo::class).toList()
        assertEquals(0, todosInSri.size)

        val storedSri = kolasuClient.populateSRI(partitionID) { TodoSymbolProvider(it) }
        todosInSri = storedSri.find(Todo::class).toList()
        assertEquals(6, todosInSri.size)

        // we check that also retrieving it we get back the same value, so we test persistence here
        sri = kolasuClient.loadSRI(partitionID)
        todosInSri = sri.find(Todo::class).toList()
        assertEquals(6, todosInSri.size)

        kolasuClient.performSymbolResolutionOnPartition(partitionID) { sri: SymbolRepository, nodeIdProvider: NodeIdProvider ->
            TodoScopeProvider(sri, nodeIdProvider)
        }
        todoProject2 = kolasuClient.retrieve(todoProject2ID) as TodoProject
        assertTrue(todoProject2.todos[1].prerequisite!!.referred != null)
        assertEquals("BD", todoProject2.todos[1].prerequisite!!.referred!!.name)
        assertEquals("Buy diary", todoProject2.todos[1].prerequisite!!.referred!!.description)

        assertTrue(todoProject2.todos[2].prerequisite!!.referred == null)
        assertTrue(todoProject2.todos[2].prerequisite!!.identifier == "synthetic_my-wonderful-partition_projects_0_todos_1")
    }

}

private fun KNode.setSource(source: Source) {
    this.origin = SimpleOrigin(Position(Point(1, 0), Point(1, 0), source), null)
}
