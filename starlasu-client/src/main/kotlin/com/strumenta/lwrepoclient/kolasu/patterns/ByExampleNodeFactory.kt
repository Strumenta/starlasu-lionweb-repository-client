package com.strumenta.lwrepoclient.kolasu.patterns

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PlaceholderElement
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.transformation.ASTTransformer

class InconsistencyException(description: String? = null) : Exception(description)

class VariantValues {
    val nodeValues: MutableMap<String, Node> = mutableMapOf()
    val multipleNodeValues: MutableMap<String, List<Node>> = mutableMapOf()
    val simpleValues: MutableMap<Any, Any?> = mutableMapOf()

    fun registerSimpleValueMatch(matchingSimpleValue: Any, matchedValue: Any?) {
        if (this.simpleValues.containsKey(matchingSimpleValue)) {
            if (this.simpleValues[matchingSimpleValue!!] != matchedValue) {
                throw InconsistencyException()
            }
        } else {
            this.simpleValues[matchingSimpleValue!!] = matchedValue
        }
    }

    fun registerMultipleQuotedMatch(placholderElement: PlaceholderElement, sourceElementsForMultiplePlaceholder: List<Node>) {
        multipleNodeValues[placholderElement.placeholderName!!] = sourceElementsForMultiplePlaceholder
    }
}

class ByExampleNodeFactory<S : Node, T : Node>(
    val sourceParser: (code: String) -> S,
    val targetParser: (code: String) -> T
) {

    private val examples = mutableListOf<Example<S, T>>()

    private class Example<S : Node, T : Node>(val sourceAst: S, val targetAst: T) {
        fun match(nodeToMatch: S): Boolean {
            val sameType = sourceAst!!.javaClass.isInstance(nodeToMatch)
            if (!sameType) {
                return false
            }
            return try {
                populatePlaceholders(nodeToMatch)
                true
            } catch (e: InconsistencyException) {
                false
            }
        }
        fun translate(sourceAst: S, astTransformer: ASTTransformer): T {
            // Generate new node
            val values = populatePlaceholders(sourceAst)
            val cloned = targetAst!!.clone(astTransformer, values)
            return cloned
        }
        private fun populatePlaceholders(
            sourceNode: Node,
            templateNode: Node = this.sourceAst,
            values: VariantValues = VariantValues()
        ): VariantValues {
            if (templateNode is PlaceholderElement) {
                values.nodeValues[templateNode.placeholderName!!] = sourceNode
                return values
            }
            if (sourceNode.nodeType != templateNode.nodeType) {
                throw InconsistencyException("Different node types: ${sourceNode.nodeType} and ${templateNode.nodeType}")
            }

            val sourceProperties = sourceNode.properties
            val templateProperties = templateNode!!.properties
            sourceProperties.forEach { sourceProperty ->
                val templateProperty = templateProperties.find { it.name == sourceProperty.name }
                    ?: throw IllegalStateException("The template has no property ${sourceProperty.name}. Template: $templateNode. Template properties: $templateProperties")
                valuesComparator(values, sourceProperty.value, templateProperty.value, sourceProperty.name)
            }
            return values
        }

        private val variantValues = mutableListOf<Any>()

        fun withVariant(variantValue: Any): Example<S, T> {
            variantValues.add(variantValue)
            return this as Example<S, T>
        }

        private fun valuesComparator(values: VariantValues, sourceValue: Any?, templateValue: Any?, desc: String) {
            if (sourceValue == null && templateValue == null) {
                // Nothing to do
            } else if (sourceValue is Node && templateValue is Node) {
                if (templateValue is PlaceholderElement && templateValue.multiplePlaceholderElement) {
                    throw java.lang.IllegalArgumentException("A multiple QuotedElement cannot be used where a single element is expected")
                }
                populatePlaceholders(sourceValue as Node, templateValue as Node, values)
            } else if (templateValue is ReferenceByName<*>) {
                val matchingSimpleValue = variantValues.find { it == (templateValue as ReferenceByName<*>).name }
                if (matchingSimpleValue != null) {
                    values.registerSimpleValueMatch(matchingSimpleValue, (sourceValue as ReferenceByName<*>).name)
                }
            } else if (sourceValue is Collection<*> && templateValue is Collection<*>) {
                val sourcePropertyColl = sourceValue as Collection<*>
                val templatePropertyColl = templateValue as Collection<*>

                val sourceIt = sourcePropertyColl.iterator()
                val templateIt = templatePropertyColl.iterator()
                var i = 0
                var multipleQuotedElement: PlaceholderElement? = null
                while (sourceIt.hasNext() && templateIt.hasNext() && multipleQuotedElement == null) {
                    val templateValue = templateIt.next()
                    if (templateValue is PlaceholderElement && templateValue.multiplePlaceholderElement) {
                        multipleQuotedElement = templateValue
                    } else {
                        val sourceValue = sourceIt.next()
                        valuesComparator(values, sourceValue, templateValue, "$desc[$i]")
                        i++
                    }
                }
                if (multipleQuotedElement == null && sourcePropertyColl.size != templatePropertyColl.size) {
                    throw InconsistencyException("Length of $desc")
                }
                if (multipleQuotedElement != null) {
                    val remainingTemplateElements = mutableListOf<Node>()
                    val remainingSourceElements = mutableListOf<Node>()
                    while (templateIt.hasNext()) {
                        remainingTemplateElements.add(templateIt.next() as Node)
                    }
                    while (sourceIt.hasNext()) {
                        remainingSourceElements.add(sourceIt.next() as Node)
                    }
                    if (remainingTemplateElements.size > remainingSourceElements.size) {
                        throw InconsistencyException()
                    }
                    val sourceElementsForMultipleQuote = remainingSourceElements.subList(0, remainingSourceElements.size - remainingTemplateElements.size)
                    values.registerMultipleQuotedMatch(multipleQuotedElement, sourceElementsForMultipleQuote)
                    val sourceElementsToBeMatchedToNextTemplateElements = remainingSourceElements.subList(remainingSourceElements.size - remainingTemplateElements.size, remainingSourceElements.size)
                    val sourceIt = sourceElementsToBeMatchedToNextTemplateElements.iterator()
                    while (sourceIt.hasNext() && templateIt.hasNext() && multipleQuotedElement == null) {
                        val templateValue = templateIt.next()
                        if (templateValue is PlaceholderElement && templateValue.multiplePlaceholderElement) {
                            throw java.lang.IllegalArgumentException("You cannot have two multiple quoted elements as siblings")
                        } else {
                            val sourceValue = sourceIt.next()
                            valuesComparator(values, sourceValue, templateValue, "$desc[$i]")
                            i++
                        }
                    }
                }
            } else {
                val matchingSimpleValue = variantValues.find { it == templateValue }
                if (matchingSimpleValue != null) {
                    // we recognized a value corresponding to a variant
                    // now we should ensure that it stays the same in all occurrences of the original
                    // template
                    // for example: 8 + 8
                    // if 8 is a variant value, we are looking for sums where left and right are the same

                    // if we have not already encountered a value for this variant we are going to set it,
                    // otherwise we are going to verify it is consistent

                    values.registerSimpleValueMatch(matchingSimpleValue, sourceValue)
                } else {
                    // nothing to do, perhaps check the values match and that's it
                    if (sourceValue != templateValue) {
                        throw InconsistencyException("On property $desc")
                    }
                }
            }
        }
    }

    fun example(sourceCode: String, targetCode: String, variants: List<Any> = emptyList()): ByExampleNodeFactory<S, T> {
        val example = Example(sourceParser(sourceCode), targetParser(targetCode))
        variants.forEach { example.withVariant(it) }
        examples.add(example)
        return this
    }

    fun example(sourceAST: S, targetCode: String, variants: List<Any> = emptyList()): ByExampleNodeFactory<S, T> {
        val example = Example(sourceAST, targetParser(targetCode))
        variants.forEach { example.withVariant(it) }
        examples.add(example)
        return this
    }

    fun asNodeFactory(): (S, ASTTransformer) -> T? {
        return { rpgAst: S, astTransformer: ASTTransformer ->
            val example = examples.find { example -> example.match(rpgAst) }
            example?.translate(rpgAst, astTransformer) ?: throw IllegalArgumentException("No matching example found")
        }
    }
}
