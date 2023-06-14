package com.idunnololz.summit.util

import com.jakewharton.disklrucache.DiskLruCache
import java.io.InputStream
import java.io.OutputStreamWriter

class LruDataCache(private val diskLruCache: DiskLruCache) : IDataCache {
    override fun cacheData(key: String, s: String?) {
        s ?: return

        val outputStream = diskLruCache.edit(key).newOutputStream(0)
        OutputStreamWriter(outputStream, "UTF-8").use {
            it.write(s)
        }
    }

    override fun cacheData(key: String, s: InputStream?) {
        s ?: return

        val editor = diskLruCache.edit(key)
        editor.newOutputStream(0).use {
            s.copyTo(it)
        }
        editor.commit()
    }

    override fun hasCache(key: String): Boolean =
        diskLruCache.get(key) != null

    override fun hasFreshCache(key: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasFreshCache(key: String, freshTime: Long): Boolean {
        TODO("Not yet implemented")
    }

    override fun getCachedDate(key: String): Long {
        TODO("Not yet implemented")
    }

    override fun getCachedData(key: String): String =
        diskLruCache.get(key).getInputStream(0).reader().use {
            it.readText()
        }

    override fun getCachedDataStream(key: String): InputStream =
        diskLruCache.get(key).getInputStream(0)

    override fun evict(key: String) {
        diskLruCache.remove(key)
    }

    fun delete() {
        diskLruCache.delete()
    }

}