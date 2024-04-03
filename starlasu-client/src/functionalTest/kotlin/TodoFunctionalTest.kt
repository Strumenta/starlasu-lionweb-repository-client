import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.SyntheticSource
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository
import com.strumenta.lwrepoclient.kolasu.KolasuClient
import com.strumenta.lwrepoclient.kolasu.performSymbolResolutionOnPartition
import com.strumenta.lwrepoclient.kolasu.populateSRI
import junit.framework.TestCase.assertTrue
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
        kolasuClient.sourceBasedNodeTypes.add(TodoProject::class)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val todoAccount = TodoAccount(mutableListOf())
        // By default the partition IDs are derived from the source
        todoAccount.source = SyntheticSource("my-wonderful-partition")
        kolasuClient.createPartition(todoAccount)

        val expectedPartitionId = kolasuClient.idFor(todoAccount)
        assertEquals("partition_synthetic_my-wonderful-partition", expectedPartitionId)

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
        // todoProject.assignParents()

        todoProject.source = SyntheticSource("TODO Project A")
        kolasuClient.createNode(todoProject, containerID = expectedPartitionId, containment = TodoAccount::projects)

        // I can retrieve the entire partition
        todoAccount.projects.add(todoProject)
        // todoAccount.assignParents()
        val retrievedTodoAccount = kolasuClient.getNode(expectedPartitionId)
        assertEquals(todoAccount, retrievedTodoAccount)

        // I can retrieve just a portion of that partition. In that case the parent of the root of the
        // subtree will appear null
        val expectedProjectId = kolasuClient.idFor(todoProject)
        assertEquals("source_synthetic_TODO_Project_A", expectedProjectId)
        val retrievedTodoProject = kolasuClient.getNode(expectedProjectId)
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
        kolasuClient.sourceBasedNodeTypes.add(TodoProject::class)

        assertEquals(emptyList(), kolasuClient.getPartitionIDs())

        // We create an empty partition
        val todoAccount = TodoAccount(mutableListOf())
        // By default the partition IDs are derived from the source
        todoAccount.source = SyntheticSource("my-wonderful-partition")
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
        todoProject.source = SyntheticSource("MyProject")
        kolasuClient.createNode(todoProject, todoAccount, containment = TodoAccount::projects)

        assertEquals("partition_synthetic_my-wonderful-partition", kolasuClient.idFor(todoAccount))
        assertEquals("source_synthetic_MyProject", kolasuClient.idFor(todoProject))
        assertEquals("synthetic_MyProject_root_todos_0", kolasuClient.idFor(todoProject.todos[0]))
        assertEquals("synthetic_MyProject_root_todos_1", kolasuClient.idFor(todoProject.todos[1]))
        assertEquals("synthetic_MyProject_root_todos_2", kolasuClient.idFor(todoProject.todos[2]))
    }

    @Test
    fun sourceIsRetrievedCorrectly() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(todoLanguage)
        kolasuClient.sourceBasedNodeTypes.add(TodoProject::class)

        // We create an empty partition
        val todoAccount = TodoAccount(mutableListOf())
        todoAccount.source = SyntheticSource("my-wonderful-partition")
        val todoAccountId = kolasuClient.createPartition(todoAccount)

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
        todoProject1.source = SyntheticSource("Project1")
        val todoProject1ID = kolasuClient.createNode(todoProject1, todoAccount, containment = TodoAccount::projects)

        val todoProject2 =
            TodoProject(
                "My other errands list",
                mutableListOf(
                    Todo("BD", "Buy diary"),
                    Todo("WD", "Write in diary", ReferenceByName("BD")),
                    Todo("garbage-in", "Produce more garbage", ReferenceByName("garbage-out")),
                ),
            )
        todoProject2.assignParents()
        todoProject2.source = SyntheticSource("Project2")
        val todoProject2ID = kolasuClient.createNode(todoProject2, todoAccount, containment = TodoAccount::projects)

        val retrievedPartition = kolasuClient.getNode(todoAccountId)

        // When retrieving the entire partition, the source should be set correctly, producing the right node id
        assertEquals(todoProject1ID, kolasuClient.idFor(retrievedPartition.children[0]))
        assertEquals(todoProject2ID, kolasuClient.idFor(retrievedPartition.children[1]))
    }

    @Test
    fun symbolResolution() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(todoLanguage)
        kolasuClient.sourceBasedNodeTypes.add(TodoProject::class)

        // We create an empty partition
        val todoAccount = TodoAccount(mutableListOf())
        todoAccount.source = SyntheticSource("my-wonderful-partition")
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
        todoProject1.source = SyntheticSource("Project1")
        kolasuClient.createNode(todoProject1, todoAccount, containment = TodoAccount::projects)

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
        todoProject2.source = SyntheticSource("Project2")
        val todoProject2ID = kolasuClient.createNode(todoProject2, todoAccount, containment = TodoAccount::projects)

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
            TodoScopeProvider(sri)
        }
        todoProject2 = kolasuClient.getNode(todoProject2ID) as TodoProject
        assertTrue(todoProject2.todos[1].prerequisite!!.referred != null)
        assertEquals("BD", todoProject2.todos[1].prerequisite!!.referred!!.name)
        assertEquals("Buy diary", todoProject2.todos[1].prerequisite!!.referred!!.description)

        assertTrue(todoProject2.todos[2].prerequisite!!.referred == null)
        assertTrue(todoProject2.todos[2].prerequisite!!.identifier == "synthetic_Project1_root_projects_0_todos_1")
    }

    @Test
    fun getNodesWithProxyParent() {
        val kolasuClient = KolasuClient(port = modelRepository!!.firstMappedPort, debug = true)
        kolasuClient.registerLanguage(todoLanguage)
        kolasuClient.sourceBasedNodeTypes.add(TodoProject::class)

        // We create an empty partition
        val todoAccount = TodoAccount(mutableListOf())
        todoAccount.source = SyntheticSource("my-wonderful-partition")
        val todoAccountId = kolasuClient.createPartition(todoAccount)

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
        todoProject1.source = SyntheticSource("Project1")
        val todoProject1ID = kolasuClient.createNode(todoProject1, todoAccount, containment = TodoAccount::projects)

        val todoProject1RetrievedWithoutProxyParent = kolasuClient.getNode(todoProject1ID, withProxyParent = false)
        assertEquals(null, todoProject1RetrievedWithoutProxyParent.parent)
        val todoProject1RetrievedWithProxyParent = kolasuClient.getNode(todoProject1ID, withProxyParent = true)
        assertEquals(todoAccountId, kolasuClient.idFor(todoProject1RetrievedWithProxyParent.parent!!))
    }
}
