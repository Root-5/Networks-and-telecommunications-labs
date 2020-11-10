package utils

import java.lang.ClassCastException

class MyPair<T1, T2>(private val a: T1, private val b: T2) {
    override fun equals(other: Any?): Boolean {
        var obj: MyPair<T1, T2>? = null
        try {
            obj = other as MyPair<T1, T2>
        } catch (ex:ClassCastException) {
            return false
        }
        if (a != null) {
            if (b != null) {
                return a == obj.a && b == obj.b
            }
        }
        return false
    }

    override fun hashCode(): Int {
        return a.hashCode()+1 + b.hashCode()+1
    }
}