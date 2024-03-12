package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.ids.IDLogic
import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.ids.SimpleSourceIdProvider
import com.strumenta.kolasu.ids.SourceIdProvider
import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.lionweb.isPartition
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.containingProperty
import com.strumenta.kolasu.model.indexInContainingProperty
import io.lionweb.lioncore.java.utils.CommonChecks
import kotlin.reflect.KClass

class DefaultLionWebRepositoryNodeIdProvider(
    val sourceBasedNodeTypes: Set<KClass<*>>,
    var sourceIdProvider: SourceIdProvider = SimpleSourceIdProvider(),
) :
    NodeIdProvider {
    protected open fun partitionId(kNode: Node): String {
        require(kNode.parent == null)

        if (kNode is IDLogic) {
            return "partition_${(kNode as IDLogic).calculatedID}"
        } else {
            require(kNode.source != null) {
                "When calculating the partitionId we either need a not with IDLogic or with a source: $kNode"
            }
            return "partition_${sourceIdProvider.sourceId(kNode.source)}"
        }
    }

    override fun id(kNode: Node): String {
        val id =
            when {
                kNode.isPartition -> partitionId(kNode)
                kNode is IDLogic -> kNode.calculatedID
                sourceBasedNodeTypes.contains(kNode::class) -> "source_" + sourceIdProvider.sourceId(kNode.source)
                else -> {
                    require(kNode.source != null) {
                        "Node $kNode is not a partition, it does not implement IDLogic, and it has not a source set, therefore it " +
                            "cannot be given a proper ID"
                    }
                    "${sourceIdProvider.sourceId(kNode.source)}_${kNode.positionalID}"
                }
            }
        if (!CommonChecks.isValidID(id)) {
            throw IllegalStateException("An invalid LionWeb Node ID has been produced ($id)")
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
