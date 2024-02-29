package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.lionweb.KNode
import java.util.IdentityHashMap

/**
 * This NodeIdProvider remembers IDs explicitly set. This is useful because we often insert nodes in an existing
 * tree. In the moment we do the insertion we know where the Node will end up and we can calculate the ID based on
 * that. Later, if we access that Node without that contextual information we would not be able to recalculate the
 * same Node ID.
 */
class OverridableNodeIdProvider(private val kolasuClient: KolasuClient) : NodeIdProvider {
    private val overrides = IdentityHashMap<KNode, String>()

    override fun id(kNode: KNode): String {
        return if (overrides.containsKey(kNode)) {
            overrides[kNode]!!
        } else {
            kolasuClient.baseIdProvider.id(kNode)
        }
    }

    operator fun set(
        kNode: KNode,
        id: String,
    ) {
        overrides[kNode] = id
    }

    fun clearOverrides() {
        overrides.clear()
    }
}
