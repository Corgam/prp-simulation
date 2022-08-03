package me.mbe.prp.base

import java.time.ZonedDateTime

abstract class CacheWithDate<K, V>(protected val gen: (k: K, date: ZonedDateTime?) -> V) {
    abstract fun get(k: K, date: ZonedDateTime?): V
    abstract fun invalidate(k: K)
    abstract fun invalidate()
}

abstract class Cache<K, V>(protected val gen: (k: K) -> V) {
    abstract fun get(k: K): V
    abstract fun invalidate(k: K)
    abstract fun invalidate()
}

class MultiElementCache<K, V>(gen: (k: K) -> V) : Cache<K, V>(gen) {
    private val c = LinkedHashMap<K, V>()

    override fun get(k: K): V {
        return c.getOrPut(k) { gen(k) }
    }

    override fun invalidate(k: K) {
        c.remove(k)
    }

    override fun invalidate() {
        c.clear()
    }
}

class OneElementCache<K, V>(gen: (k: K) -> V) : Cache<K, V>(gen) {
    private var cacheKey: K? = null
    private var cacheValue: V? = null

    override fun get(k: K): V {
        if (k.hashCode() != cacheKey.hashCode() || k != cacheKey) {
            cacheKey = k
            cacheValue = gen(k)
        }
        return cacheValue!!
    }

    override fun invalidate(k: K) {
        if (k != cacheKey) return
        invalidate()
    }

    override fun invalidate() {
        cacheKey = null
        cacheValue = null
    }

}

class OneElementCacheWithDate<K, V>(gen: (k: K, date: ZonedDateTime?) -> V) : CacheWithDate<K, V>(gen) {
    private var cacheKey: K? = null
    private var cacheValue: V? = null

    override fun get(k: K, date: ZonedDateTime?): V {
        if (k.hashCode() != cacheKey.hashCode() || k != cacheKey) {
            cacheKey = k
            cacheValue = gen(k, date)
        }
        return cacheValue!!
    }

    override fun invalidate(k: K) {
        if (k != cacheKey) return
        invalidate()
    }

    override fun invalidate() {
        cacheKey = null
        cacheValue = null
    }

}
