import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.lionweb.LionWebPartition
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node

@LionWebPartition
data class TodoAccount(val projects: MutableList<TodoProject>) : Node()

data class TodoProject(override var name: String, val todos: MutableList<Todo> = mutableListOf()) : Node(), Named

data class Todo(var description: String) : Node()

val todoLanguage =
    KolasuLanguage("TodoLanguage").apply {
        addClass(TodoAccount::class)
        addClass(TodoProject::class)
    }
