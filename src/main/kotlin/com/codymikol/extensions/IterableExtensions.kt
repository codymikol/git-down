package com.codymikol.extensions

public inline fun <T> List<T>.indexOfLastOrNull(predicate: (T) -> Boolean): Int? = when(val idx = indexOfLast { predicate(it) }) {
    -1 -> null
    else -> idx
}

public inline fun <T> List<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? = when(val idx = indexOfFirst { predicate(it) }) {
    -1 -> null
    else -> idx
}