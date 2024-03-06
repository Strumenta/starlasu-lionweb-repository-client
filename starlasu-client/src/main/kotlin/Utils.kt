package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Source

fun Node.withSource(source: Source): Node {
    this.setSourceForTree(source)
    require(this.source === source)
    return this
}

// class ModifiedSimpleSourceIdProvider : SourceIdProvider {
//    private val wrapped = SimpleSourceIdProvider()
//
//    override fun sourceId(source: Source?): String {
//        if (source is CodeBaseSource) {
//            return "codebase_${source.codebaseName.lionWebClean()}_rpath_${source.relativePath.lionWebClean()}"
//        }
//        if (source is LionWebRootSource) {
//            return source.sourceId
//        }
//        return wrapped.sourceId(source)
//    }
// }

// private fun String.lionWebClean(): String {
//    return this.replace("/", "_").replace(".", "_").replace(" ", "_")
// }
