package com.example.trilib

interface Cloneable<T> {
    fun clone(): T
}

inline fun <reified T : Cloneable<T>> cloneArray(from: Array<T>): Array<T> {
    return Array(from.size) { index ->
        from[index].clone()
    }
}