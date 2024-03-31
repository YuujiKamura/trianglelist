package com.jpaver.trianglelist.util

interface Cloneable<T> {
    fun clone(): T
}

inline fun <reified T : Cloneable<T>> cloneArray(from: Array<T>): Array<T> {
    return Array(from.size) { index ->
        from[index].clone()
    }
}