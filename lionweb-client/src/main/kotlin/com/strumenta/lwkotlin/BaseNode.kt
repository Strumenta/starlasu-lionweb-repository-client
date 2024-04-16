package com.strumenta.lwkotlin

import io.lionweb.lioncore.java.language.Containment
import io.lionweb.lioncore.java.model.Node
import io.lionweb.lioncore.java.model.impl.DynamicNode
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Implementation

abstract class BaseNode : DynamicNode(null, null) {

    open fun calculateID() : String? = null

    override fun getID(): String? {
        return calculateID() ?: super.getID()
    }

    fun <C: Node>multipleContainment(name: String) : MutableList<C> {
        return ContainmentList(this, concept.requireContainmentByName(name))
    }

    protected fun <P: BaseNode, C: Node> singleContainment(containmentName: String): ReadWriteProperty<P, C?> {
        return object : ReadWriteProperty<P, C?> {
            override fun getValue(thisRef: P, property: KProperty<*>): C? {
                return thisRef.getOnlyChildByContainmentName(containmentName) as C?
            }

            override fun setValue(thisRef: P, property: KProperty<*>, value: C?) {
                val containment = thisRef.concept.requireContainmentByName(containmentName)
                thisRef.addChild(containment, value)
            }

        }
    }

    protected fun <P: BaseNode, V: Any> property(propertyName: String): ReadWriteProperty<P, V?> {
        return object : ReadWriteProperty<P, V?> {
            override fun getValue(thisRef: P, property: KProperty<*>): V? {
                return thisRef.getPropertyValueByName(propertyName) as V?
            }

            override fun setValue(thisRef: P, property: KProperty<*>, value: V?) {
                thisRef.setPropertyValueByName(propertyName, value)
            }

        }
    }
}

private class ContainmentList<E: Node>(private val node: DynamicNode, private val containment: Containment) : MutableList<E>{
    override val size: Int
        get() = node.getChildren(containment).size

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun get(index: Int): E {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): MutableIterator<E> {
        TODO("Not yet implemented")
    }

    override fun listIterator(): MutableListIterator<E> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        TODO("Not yet implemented")
    }

    override fun removeAt(index: Int): E {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        TODO("Not yet implemented")
    }

    override fun set(index: Int, element: E): E {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun remove(element: E): Boolean {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: E): Int {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: E): Int {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun contains(element: E): Boolean {
        TODO("Not yet implemented")
    }

    override fun addAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun add(index: Int, element: E) {
        TODO("Not yet implemented")
    }

    override fun add(element: E): Boolean {
        TODO("Not yet implemented")
    }

}