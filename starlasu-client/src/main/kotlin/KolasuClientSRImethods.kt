package com.strumenta.lwrepoclient.kolasu

import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.lionweb.LWNode
import com.strumenta.kolasu.lionweb.StarLasuLWLanguage
import io.lionweb.lioncore.java.language.Concept

fun KolasuClient.getASTRoots(aLWNode: LWNode): Sequence<KNode> {
    val res = mutableListOf<KNode>()

    fun exploreForASTs(aLWNode: LWNode) {
        val isKNode: Boolean = isKolasuConcept(aLWNode.concept)
        if (isKNode) {
            res.add(toKolasuNode(aLWNode))
        } else {
            aLWNode.children.forEach { exploreForASTs(it) }
        }
    }

    exploreForASTs(aLWNode)
    return res.asSequence()
}

fun isKolasuConcept(concept: Concept): Boolean {
    return concept.allAncestors().contains(StarLasuLWLanguage.ASTNode)
}
