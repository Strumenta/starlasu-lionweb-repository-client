package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.ids.Coordinates
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

const val PARTITION_PREFIX = "partition_"
const val SOURCE_PREFIX = "source_"
const val ROOT_POSTFIX = "_root"

class DefaultLionWebRepositoryNodeIdProvider(
    val sourceBasedNodeTypes: Set<KClass<*>>,
    var sourceIdProvider: SourceIdProvider = SimpleSourceIdProvider(),
) :
    NodeIdProvider {
    protected open fun partitionId(kNode: Node): String {
        require(kNode.parent == null)

        if (kNode is IDLogic) {
            return "$PARTITION_PREFIX${(kNode as IDLogic).calculatedID(null)}"
        } else {
            require(kNode.source != null) {
                "When calculating the partitionId we either need a not with IDLogic or with a source: $kNode"
            }
            return "$PARTITION_PREFIX${sourceIdProvider.sourceId(kNode.source)}"
        }
    }

    override fun idUsingCoordinates(
        kNode: Node,
        coordinates: Coordinates,
    ): String {
        val id =
            when {
                kNode.isPartition -> partitionId(kNode)
                kNode is IDLogic -> kNode.calculatedID(coordinates)
                sourceBasedNodeTypes.contains(kNode::class) -> SOURCE_PREFIX + sourceIdProvider.sourceId(kNode.source)
                else -> {
                    require(kNode.source != null) {
                        "Node $kNode is not a partition, it does not implement IDLogic, and it has not a source set, therefore it " +
                            "cannot be given a proper ID"
                    }
                    "${sourceIdProvider.sourceId(kNode.source)}_${kNode.positionalID}"
                }
            }
        if (!kNode.isPartition && id.startsWith(PARTITION_PREFIX)) {
            throw IllegalStateException("The node is not a partition but its ID has the prefix used for partitions")
        }
        if (!sourceBasedNodeTypes.contains(kNode::class) && id.startsWith(SOURCE_PREFIX)) {
            throw IllegalStateException(
                "The node is not a source based node but its ID has the prefix used for " +
                    "source based nodes",
            )
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
