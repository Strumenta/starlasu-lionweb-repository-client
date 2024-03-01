import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.lionweb.LionWebPartition
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.semantics.scope.provider.declarative.DeclarativeScopeProvider
import com.strumenta.kolasu.semantics.scope.provider.declarative.scopeFor
import com.strumenta.kolasu.semantics.symbol.provider.declarative.DeclarativeSymbolProvider
import com.strumenta.kolasu.semantics.symbol.provider.declarative.symbolFor
import com.strumenta.kolasu.semantics.symbol.repository.SymbolRepository

@LionWebPartition
data class TodoAccount(val projects: MutableList<TodoProject>) : Node()

data class TodoProject(override var name: String, val todos: MutableList<Todo> = mutableListOf()) : Node(), Named

data class Todo(
    override var name: String,
    var description: String,
    val prerequisite: ReferenceByName<Todo>? = null,
) : Node(), Named {
    constructor(name: String) : this(name, name)
}

val todoLanguage =
    KolasuLanguage("TodoLanguage").apply {
        addClass(TodoAccount::class)
        addClass(TodoProject::class)
    }

class TodoSymbolProvider(nodeIdProvider: NodeIdProvider) : DeclarativeSymbolProvider(
    nodeIdProvider,
    symbolFor<Todo> {
        this.name(it.node.name)
    },
)

class TodoScopeProvider(val sri: SymbolRepository) : DeclarativeScopeProvider(
    scopeFor(Todo::prerequisite) {
        // We first consider local todos, as they may shadow todos from other projects
        (it.node.parent as TodoProject).todos.forEach {
            define(it)
        }
        // We then consider all symbols from the sri. Note that nodes of the current project
        // appear both as nodes and as symbols
        sri.find(Todo::class).forEach {
            define(it.name!!, it)
        }
    },
)
