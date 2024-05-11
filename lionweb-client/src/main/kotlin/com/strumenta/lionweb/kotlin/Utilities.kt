package com.strumenta.lionweb.kotlin

import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.model.HasSettableParent
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.impl.DynamicNode
import kotlin.random.Random
import kotlin.reflect.KClass

/**
 * Create a Dynamic Node with the given Concept and a random node ID.
 */
fun Concept.dynamicNode(nodeId: String = "node-id-rand-${Random.nextInt()}"): DynamicNode {
    return DynamicNode(nodeId, this)
}

fun <N> N.withParent(parent: Node?): N where N : Node, N : HasSettableParent {
    this.setParent(parent)
    return this
}

fun <N : Node> Node.walkDescendants(kClass: KClass<N>): Sequence<N> {
    return sequence {
        this@walkDescendants.children.forEach { child ->
            if (kClass.isInstance(child)) {
                yield(child as N)
            }
            yieldAll(child.walkDescendants(kClass))
        }
    }
}
