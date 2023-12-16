package com.strumenta.starlasu.lwrepoclient.kolasuexample

import com.strumenta.kolasu.language.KolasuLanguage
import com.strumenta.kolasu.model.Named
import com.strumenta.kolasu.model.Node
import com.strumenta.starlasu.lwrepoclient.Multiplicity
import com.strumenta.starlasu.lwrepoclient.addConcept
import com.strumenta.starlasu.lwrepoclient.addContainment
import com.strumenta.starlasu.lwrepoclient.lwLanguage
import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.LionCoreBuiltins

data class TodoProject(override var name: String, val todos: MutableList<Todo> = mutableListOf()) : Node(), Named
data class Todo(var description: String) : Node()

val todoLanguage = KolasuLanguage("TodoLanguage").apply {
    addClass(TodoProject::class)
}

