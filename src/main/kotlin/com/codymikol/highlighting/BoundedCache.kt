package com.codymikol.highlighting

/**
 * A fixed-capacity, thread-safe LRU cache. Plain unbounded caches keyed by file content (as
 * [FullFileLineHighlighter]'s is) would otherwise grow forever across a long-running session as a
 * user edits and reviews diffs, since every distinct version of every file viewed becomes its own
 * key that's never removed.
 */
class BoundedCache<K, V>(private val maxSize: Int) {

    private val map = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > maxSize
    }

    @Synchronized
    fun getOrPut(key: K, compute: () -> V): V {
        map[key]?.let { return it }
        val value = compute()
        map[key] = value
        return value
    }

}
