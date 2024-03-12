package com.strumenta.lwrepoclient.kolasu.patterns

import com.strumenta.kolasu.lionweb.KNode
import com.strumenta.kolasu.model.assignParents

data class Pattern(val node: KNode?) {
    init {
        node?.assignParents()
    }
    private val variableValues: MutableSet<Any> = mutableSetOf()
    fun withNode(node: KNode?) : Pattern {
        return this.copy(node = node).also {
            it.variableValues.addAll(this.variableValues)
        }
    }

    fun withVariable(variableValue: Any) {
        variableValues.add(variableValue)
    }

    fun isVariableValue(variableValue: Any?): Boolean {
        return variableValues.contains(variableValue)
    }
}

class PatternInstance {
    private val values = mutableMapOf<Any, Any?>()

    fun hasVariableValue(variableValue: Any): Boolean {
        return values.containsKey(variableValue)
    }
    fun valueFor(variableValue: Any): Any? {
        require(hasVariableValue(variableValue))
        return values[variableValue]
    }

    fun tryToSetVariableValue(variableValue: Any, actualValue: Any?): Boolean {
        if (!hasVariableValue(variableValue)) {
            values[variableValue] = actualValue
            return true
        } else return valueFor(variableValue) == actualValue
    }
}