package com.strumenta.lwrepoclient.base

import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.serialization.JsonSerialization
import java.io.File

/**
 * Perform some sanity checks on the tree. This is mostly useful while debugging the export to LionWeb.
 * Eventually this could be dropped or controlled by some flag.
 */
fun treeSanityChecks(
    node: Node,
    parents: MutableMap<String, String?> = mutableMapOf(),
    jsonSerialization: JsonSerialization
) {
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
            treeSanityChecks(it, parents, jsonSerialization)
        }
    } catch (t: Throwable) {
        File("error.json").writeText(jsonSerialization.serializeTreesToJsonString(node.root))
        throw RuntimeException(t)
    }
}
