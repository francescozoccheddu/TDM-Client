package com.francescozoccheddu.tdmclient.utils

class FixedSizeSortedQueue<Type : Any>(val size: Int, val comparer: (Type, Type) -> Boolean) : MutableIterable<Type> {

    companion object {

        fun <Type : Comparable<Type>> by(size: Int, reverse: Boolean) =
            FixedSizeSortedQueue<Type>(size) { a, b ->
                val comparison = a.compareTo(b)
                if (reverse) comparison > 0 else comparison < 0
            }

        fun <Type : Any, ComparableType : Comparable<ComparableType>> by(
            size: Int,
            reverse: Boolean,
            map: (Type) -> ComparableType
        ) = FixedSizeSortedQueue<Type>(size) { a, b ->
            val comparison = map(a).compareTo(map(b))
            if (reverse) comparison > 0 else comparison < 0
        }

    }

    private class Node<Type : Any> {
        var previous: Node<Type>? = null
        var next: Node<Type>? = null
        lateinit var value: Type
    }

    private val nodePool = Array<Node<Type>?>(size) { Node() }
    private var poolSize = size

    private fun borrowNode(value: Type, previous: Node<Type>?, next: Node<Type>?) =
        nodePool[--poolSize].apply { this!!.value = value }

    private fun returnNode(node: Node<Type>) {
        nodePool[poolSize++] = node
    }

    val length
        get() = size - poolSize

    private var head: Node<Type>? = null
    private var tail: Node<Type>? = null

    private fun addFirst(value: Type) {
        head = borrowNode(value, null, head)
        if (tail == null)
            tail = head
    }

    private fun addBefore(node: Node<Type>, value: Type) {
        if (node == head)
            addFirst(value)
        else {
            val newNode = borrowNode(value, node.previous, node)
            node.previous?.next = newNode
            node.previous = newNode
        }
    }

    private fun addAfter(node: Node<Type>, value: Type) {
        if (node == tail)
            addLast(value)
        else {
            val newNode = borrowNode(value, node, node.next)
            node.next?.previous = newNode
            node.next = newNode
        }
    }

    private fun addLast(value: Type) {
        tail = borrowNode(value, tail, null)
        if (head == null)
            head = tail
    }

    private fun remove(node: Node<Type>) {
        if (head == node)
            head = node.next
        if (tail == node)
            tail = node.previous
        node.next?.previous = node.previous
        node.previous?.next = node.next
        returnNode(node)
    }

    private fun insert(value: Type, seek: Node<Type>): Node<Type>? {
        if (poolSize == 0)
            if (comparer(value, tail!!.value))
                remove(tail!!)
            else return null
        val firstComparison = comparer(value, seek.value)
        var node: Node<Type>? = seek
        var lastNode: Node<Type>
        do {
            lastNode = node!!
            node = if (firstComparison) lastNode.previous else lastNode.next
        } while (node != null && firstComparison == comparer(value, node.value))
        if (firstComparison) {
            addBefore(lastNode, value)
            return lastNode.previous
        }
        else {
            addAfter(lastNode, value)
            return lastNode.next
        }
    }

    private fun insert(value: Type, reverseSeek: Boolean): Node<Type>? {
        return if (length == 0) {
            addFirst(value)
            head
        }
        else
            insert(value, next(null, reverseSeek)!!)
    }

    private fun next(node: Node<Type>?, reverse: Boolean) =
        if (node == null) {
            if (reverse) tail else head
        }
        else {
            if (reverse) node.previous else node.next
        }


    private open inner class QueueIterator(val reverse: Boolean) : Iterator<Type> {

        protected var current: Node<Type>? = null

        override fun hasNext() = next(current, reverse) != null

        override fun next(): Type {
            current = next(current, reverse)
            return current!!.value
        }
    }

    private inner class MutableQueueIterator(reverse: Boolean) : QueueIterator(reverse), MutableIterator<Type> {

        override fun remove() {
            remove(current!!)
        }

    }

    fun add(value: Type, reverseSeek: Boolean = false) = insert(value, reverseSeek) != null

    fun addLocalized(values: Iterable<Type>, reverseSeek: Boolean = false): Int {
        var inserts = 0
        var seek: Node<Type>? = null
        for (value in values) {
            seek = if (seek != null) {
                inserts++
                insert(value, seek)
            }
            else insert(value, reverseSeek)
        }
        return inserts
    }

    override operator fun iterator() = mutableIterator(false)

    fun readOnlyIterator(reverse: Boolean = false): Iterator<Type> = QueueIterator(reverse)
    fun mutableIterator(reverse: Boolean = false): MutableIterator<Type> = MutableQueueIterator(reverse)

    val first get() = head?.value
    val last get() = tail?.value

}


