package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.lionweb.LionWebNodeIdProvider
import com.strumenta.kolasu.lionweb.SimpleSourceIdProvider
import com.strumenta.kolasu.lionweb.SourceIdProvider
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.SyntheticSource
import com.strumenta.kolasu.model.containingProperty
import com.strumenta.kolasu.model.indexInContainingProperty
import io.lionweb.lioncore.java.utils.CommonChecks

class DefaultLionWebRepositoryNodeIdProvider(var sourceIdProvider: SourceIdProvider = SimpleSourceIdProvider()) :
    LionWebNodeIdProvider {
    protected open fun partitionId(kNode: Node): String {
        require(kNode.parent == null)
        require(kNode.source != null)
        // TODO update SimpleSourceIdProvider
        if (kNode.source is SyntheticSource) {
            return "synthetic_" + (kNode.source as SyntheticSource).description
        }
        return sourceIdProvider.sourceId(kNode.source)
    }

    override fun id(kNode: Node): String {
        if (kNode.isPartition) {
            return partitionId(kNode)
        }
        val id = "${sourceIdProvider.sourceId(kNode.source)}_${kNode.positionalID}"
        if (!CommonChecks.isValidID(id)) {
            throw IllegalStateException("An invalid LionWeb Node ID has been produced")
        }
        return id
    }

    private val KNode.positionalID: String
        get() {
            return if (this.parent == null) {
                "root"
            } else {
                val cp = this.containingProperty()!!
                val postfix = if (cp.multiple) "${cp.name}_${this.indexInContainingProperty()!!}" else cp.name
                "${this.parent!!.positionalID}_$postfix"
            }
        }
}
