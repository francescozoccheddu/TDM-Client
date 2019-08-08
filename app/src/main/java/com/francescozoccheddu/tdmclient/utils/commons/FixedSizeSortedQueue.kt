package com.francescozoccheddu.tdmclient.utils.commons

class FixedSizeSortedQueue<Type>(val size: Int, val comparer: (Type, Type) -> Boolean) : MutableIterable<Type> {

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

    private class Node<Type> {
        var previous: Node<Type>? = null
        var next: Node<Type>? = null
        var value: Type by LateInit()
    }

    private val nodePool = Array<Node<Type>?>(size) { Node() }
    private var poolSize = size

    private fun borrowNode(value: Type, previous: Node<Type>?, next: Node<Type>?) =
        nodePool[--poolSize]!!.apply {
            this.value = value
            this.next = next
            this.previous = previous
            previous?.next = this
            next?.previous = this
        }

    private fun returnNode(node: Node<Type>) {
        nodePool[poolSize++] = node
    }

    val length
        get() = size - poolSize

    // TODO Simplify
    // TODO Link inside borrowNode?

    private var head: Node<Type>? = null
    private var tail: Node<Type>? = null
    private val tailOrHead get() = tail ?: head

    private fun addFirst(value: Type) {
        if (head == null) {
            head = borrowNode(value, null, null)
            tail = null
        }
        else {
            if (tail == null)
                tail = borrowNode(value, head, null)
            else {
                val prevHead = head
                val newHead = borrowNode(value, null, prevHead)
                head = newHead
            }
        }
    }

    private fun addLast(value: Type) {
        if (head == null) {
            head = borrowNode(value, null, null)
            tail = null
        }
        else {
            if (tail == null)
                tail = borrowNode(value, head, null)
            else {
                val prevTail = tail
                val newTail = borrowNode(value, prevTail, null)
                tail = newTail
            }
        }
    }

    private fun addBefore(node: Node<Type>, value: Type) {
        if (node == head)
            addFirst(value)
        else
            borrowNode(value, node.previous, node)
    }

    private fun addAfter(node: Node<Type>, value: Type) {
        if (node == tailOrHead)
            addLast(value)
        else
            borrowNode(value, node, node.next)
    }

    private fun remove(node: Node<Type>) {
        val prevNode = node.previous
        val nextNode = node.next
        if (prevNode == null) {
            head = nextNode
            head?.previous = null
        }
        else if (nextNode == null) {
            tail = prevNode
            tail?.next = null
        }
        else {
            prevNode.next = nextNode
            nextNode.previous = prevNode
        }
        if (head == tail)
            tail = null
        returnNode(node)
    }

    private fun insert(value: Type, seek: Node<Type>): Node<Type>? {
        var node: Node<Type>? = seek
        if (poolSize == 0)
            if (comparer(value, tail!!.value)) {
                if (seek == tail) {
                    if (length == 1) {
                        tail!!.value = value
                        return tail
                    }
                    else
                        node = seek.previous
                }
                remove(tail!!)
            }
            else return null
        val firstComparison = comparer(value, node!!.value)
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
            if (reverse) tailOrHead else head
        }
        else {
            if (reverse) node.previous else node.next
        }


    private open inner class QueueIterator(val reverse: Boolean) : Iterator<Type> {

        protected lateinit var current: Node<Type>
        private var next: Node<Type>? = next(null, reverse)

        override fun hasNext() = next != null

        override fun next(): Type {
            current = next!!
            next = next(next, reverse)
            return current.value
        }
    }

    private inner class MutableQueueIterator(reverse: Boolean) : QueueIterator(reverse), MutableIterator<Type> {

        override fun remove() {
            remove(current)
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
    val last get() = tailOrHead?.value

    fun print(): String {
        val b = StringBuilder()
        for (i in this)
            b.append("$i   ")
        b.appendln()
        b.append("${head?.value}   ${tail?.value}")
        return b.toString()
    }

}


