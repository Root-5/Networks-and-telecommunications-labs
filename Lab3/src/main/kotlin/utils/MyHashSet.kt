package utils

import java.util.*
import kotlin.collections.HashSet

class MyHashSet<T>(private val limit: Int) {
    private val hashSet = HashSet<T>()
    private val queue = LinkedList<T>()

    public fun add(arg: T) {
        hashSet.add(arg)
        queue.add(arg)
        if (hashSet.size == limit) {
            hashSet.remove(queue.peek())
            queue.remove()
        }
    }

    public fun remove(arg: T) {
        hashSet.remove(arg)
    }

    public fun getHashSet(): HashSet<T> {
        return hashSet
    }
}