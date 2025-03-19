package com.idunnololz.summit.cache

import android.util.Log
import com.jakewharton.disklrucache.DiskLruCache
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.io.Serializable
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.NumberFormat

class SimpleDiskCache(
    dir: File,
    private val appVersion: Int,
    maxSize: Long,
)  {

    companion object {

        private val TAG = SimpleDiskCache::class.java.simpleName

        /**
         * Number of "fields"
         */
        private const val VALUE_COUNT = 3

        private const val VALUE_IDX = 0
        private const val METADATA_IDX = 1
        private const val MODIFIED_TIME_IDX = 2
    }

    var cache: DiskLruCache
        private set
    private val cacheName: String = dir.name

    init {
        cache = DiskLruCache.open(dir, appVersion, VALUE_COUNT, maxSize)
    }

    /**
     * User should be sure there are no outstanding operations.
     * @throws IOException thrown for file related errors
     */
    @Throws(IOException::class)
    fun clear() {
        val dir = cache.directory
        val maxSize = cache.maxSize
        cache.flush()
        cache.close()
        dir.deleteRecursively()
        dir.mkdir()

        cache = DiskLruCache.open(dir, appVersion, VALUE_COUNT, maxSize)
    }

    @Throws(IOException::class)
    fun getString(key: String): StringEntry? {
        return cache.get(toInternalKey(key))?.use { snapshot ->
            StringEntry(snapshot.getString(VALUE_IDX), readMetadata(snapshot))
        } ?: return null
    }

    fun hasCache(key: String): Boolean {
        val snapshot: DiskLruCache.Snapshot?
        try {
            snapshot = cache.get(toInternalKey(key))
        } catch (e: IOException) {
            return false
        }

        if (snapshot == null) return false

        snapshot.close()
        return true
    }


    fun hasFreshCache(key: String, freshTimeMs: Long): Boolean {
        return System.currentTimeMillis() - getCachedDate(key) < freshTimeMs
    }

    fun getCachedDate(key: String): Long {
        var snapshot: DiskLruCache.Snapshot? = null
        return try {
            snapshot = cache.get(toInternalKey(key))
            if (snapshot == null) {
                0
            } else {
                java.lang.Long.valueOf(
                    snapshot.getString(MODIFIED_TIME_IDX),
                )
            }
        } catch (e: Exception) {
            0
        } finally {
            snapshot?.close()
        }
    }

    @Throws(IOException::class)
    fun cacheData(key: String, s: String?) {
        put(key, s, HashMap())
        Log.d(
            TAG,
            String.format(
                "[%s] Cached data with key: %s. New cache size: %d/%d(%s) bytes.",
                cacheName,
                key,
                cache.size(),
                cache.maxSize,
                NumberFormat.getPercentInstance().format(
                    (cache.size() / cache.maxSize.toFloat()).toDouble(),
                ),
            ),
        )
    }

    @Throws(IOException::class)
    fun getCachedData(key: String): String? {
        return getString(key)?.string
    }

    @Throws(IOException::class)
    fun evict(key: String) {
        cache.remove(toInternalKey(key))
    }

    @Throws(IOException::class)
    private fun openStream(key: String, metadata: Map<String, Serializable>): OutputStream {
        val editor = cache.edit(toInternalKey(key))
        try {
            editor.set(MODIFIED_TIME_IDX, System.currentTimeMillis().toString())
            writeMetadata(metadata, editor)
            val bos = BufferedOutputStream(editor.newOutputStream(VALUE_IDX))
            return CacheOutputStream(bos, editor)
        } catch (e: IOException) {
            editor.abort()
            throw e
        }
    }

    @Throws(IOException::class)
    fun put(key: String, inputStream: InputStream) {
        put(key, inputStream, HashMap())
    }

    @Synchronized
    @Throws(IOException::class)
    fun put(key: String, inputStream: InputStream, annotations: Map<String, Serializable>) {
        openStream(key, annotations).use { outputStream ->
            val buffer = ByteArray(1024)

            while (true) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break

                outputStream.write(buffer, 0, bytesRead)
            }
        }
    }

    @Synchronized
    @Throws(IOException::class)
    fun put(key: String, value: String?, annotations: Map<String, Serializable>) {
        openStream(key, annotations).use {
            it.write(value!!.toByteArray())
        }
    }

    @Throws(IOException::class)
    private fun writeMetadata(metadata: Map<String, Serializable>, editor: DiskLruCache.Editor) {
        ObjectOutputStream(BufferedOutputStream(editor.newOutputStream(METADATA_IDX))).use {
            it.writeObject(metadata)
        }
    }

    @Throws(IOException::class)
    private fun readMetadata(snapshot: DiskLruCache.Snapshot): Map<String, Serializable> {
        var ois: ObjectInputStream? = null
        try {
            ois = ObjectInputStream(
                BufferedInputStream(
                    snapshot.getInputStream(METADATA_IDX),
                ),
            )
            @Suppress("unchecked_cast")
            return ois.readObject() as Map<String, Serializable>
        } catch (e: ClassNotFoundException) {
            throw RuntimeException(e)
        } finally {
            try {
                ois?.close()
            } catch (e: IOException) {
                // do nothing
            }
        }
    }

    private fun toInternalKey(key: String): String {
        return md5(key)
    }

    private fun md5(s: String): String {
        try {
            val m = MessageDigest.getInstance("MD5")
            m.update(s.toByteArray(charset("UTF-8")))
            val digest = m.digest()
            val bigInt = BigInteger(1, digest)
            return bigInt.toString(16)
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError()
        } catch (e: UnsupportedEncodingException) {
            throw AssertionError()
        }
    }

    private inner class CacheOutputStream(
        os: OutputStream,
        private val editor: DiskLruCache.Editor,
    ) : FilterOutputStream(os) {

        private var failed = false

        @Throws(IOException::class)
        override fun close() {
            var closeException: IOException? = null
            try {
                super.close()
            } catch (e: IOException) {
                closeException = e
            }

            if (failed) {
                editor.abort()
            } else {
                editor.commit()
            }

            if (closeException != null) throw closeException
        }

        @Throws(IOException::class)
        override fun flush() {
            try {
                super.flush()
            } catch (e: IOException) {
                failed = true
                throw e
            }
        }

        @Throws(IOException::class)
        override fun write(oneByte: Int) {
            try {
                super.write(oneByte)
            } catch (e: IOException) {
                failed = true
                throw e
            }
        }

        @Throws(IOException::class)
        override fun write(buffer: ByteArray) {
            try {
                super.write(buffer)
            } catch (e: IOException) {
                failed = true
                throw e
            }
        }

        @Throws(IOException::class)
        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            try {
                super.write(buffer, offset, length)
            } catch (e: IOException) {
                failed = true
                throw e
            }
        }
    }

    class StringEntry(
        val string: String,
        @Suppress("unused")
        val metadata: Map<String, Serializable>,
    )
}
