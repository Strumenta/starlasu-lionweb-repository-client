package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.lionweb.LWNode
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Source
import io.lionweb.lioncore.java.model.impl.DynamicNode
import io.lionweb.lioncore.java.model.impl.ProxyNode

fun Node.withSource(source: Source): Node {
    this.setSourceForTree(source)
    require(this.source === source)
    return this
}

fun LWNode.setParentID(parentID: String?) {
    val parent =
        if (parentID == null) {
            null
        } else {
            ProxyNode(parentID)
        }
    (this as DynamicNode).parent = parent
}
