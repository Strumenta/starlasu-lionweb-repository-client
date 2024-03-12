package com.strumenta.lwrepoclient.kolasu.patterns

import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.model.PropertyType

fun KNode?.matchPattern(pattern: Pattern, patternInstance: PatternInstance = PatternInstance()) : PatternInstance? {
    if (this == null) {
        return if (pattern.node == null) {
            patternInstance
        } else {
            null
        }
    }
    if (pattern.node == null) {
        return null
    }
    if (this.nodeType != pattern.node.nodeType) {
        return null
    }
    this.properties.forEach { prop ->
        when (prop.propertyType) {
            PropertyType.ATTRIBUTE -> {
                val instancePropValue = prop.value
                val patternPropValue = pattern.node.getAttributeValue(prop.name)
                if (pattern.isVariableValue(patternPropValue)) {
                    if (!patternInstance.tryToSetVariableValue(patternPropValue!!, instancePropValue)) {
                        return null
                    }
                } else {
                    if (instancePropValue != patternPropValue) {
                        return null
                    }
                }
            }
            PropertyType.REFERENCE -> {
                TODO()
            }
            PropertyType.CONTAINMENT -> {
                val instancePropValue = prop.value
                val patternPropValue = pattern.node.getAttributeValue(prop.name)
                if (prop.multiple) {
                    val instanceValues = instancePropValue as List<KNode>
                    val patternValues = patternPropValue as List<KNode>
                    if (instanceValues.size != patternValues.size) {
                        return null
                    }
                    instanceValues.forEachIndexed { index, instanceChild ->
                        val patternChild = patternValues[index]
                        if (null == instanceChild.matchPattern(pattern.withNode(patternChild), patternInstance)) {
                            return null
                        }
                    }
                } else {
                    val instanceChild = instancePropValue as KNode?
                    val patternChild = patternPropValue as KNode?
                    if (null == instanceChild.matchPattern(pattern.withNode(patternChild), patternInstance)) {
                        return null
                    }
                }
            }
        }
    }
    return patternInstance
}

fun KNode.canMatchPattern(pattern: Pattern) : Boolean {
    return this.matchPattern(pattern) != null
}