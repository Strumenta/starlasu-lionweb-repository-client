package com.strumenta.lwrepoclient.base

import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.serialization.JsonSerialization
import java.io.File

fun checkTree(node: Node, parents: MutableMap<String, String?> = mutableMapOf(), jsonSerialization: JsonSerialization) {
    try {
        if (parents.containsKey(node.id!!)) {
            throw IllegalStateException("Node with ID ${node.id} has already a parent")
        }
        parents[node.id!!] = node.parent?.id
        node.concept.allContainments().forEach { containment ->
            val childrenInContainment = containment.children.map { it.id }
            require(childrenInContainment.none { it !== null })
            require(childrenInContainment.distinct() == childrenInContainment)
        }
        node.children.forEach {
            checkTree(it, parents, jsonSerialization)
        }
    } catch (t: Throwable) {
        File("error.json").writeText(jsonSerialization.serializeTreesToJsonString(node.root))
        throw RuntimeException(t)
    }
}
