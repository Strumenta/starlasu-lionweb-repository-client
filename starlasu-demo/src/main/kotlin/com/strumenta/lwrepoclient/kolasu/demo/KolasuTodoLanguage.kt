package com.strumenta.lwrepoclient.kolasu.demo

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node

data class TodoProject(override var name: String, val todos: MutableList<Todo> = mutableListOf()) : Node(), Named
data class Todo(var description: String) : Node()

val todoLanguage = KolasuLanguage("TodoLanguage").apply {
    addClass(TodoProject::class)
}
