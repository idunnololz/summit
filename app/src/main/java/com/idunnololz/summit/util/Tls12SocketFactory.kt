package com.idunnololz.summit.util

import android.os.Build
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.security.KeyStore
import javax.net.ssl.*

/**
 * Implementation of [SSLSocketFactory] that adds [TlsVersion.TLS_1_2] as an enabled protocol for every [SSLSocket]
 * created by [delegate].
 *
 * [See this discussion for more details.](https://github.com/square/okhttp/issues/2372#issuecomment-244807676)
 *
 */
class Tls12SocketFactory(private val delegate: SSLSocketFactory) : SSLSocketFactory() {

    /**
     * Forcefully adds [TlsVersion.TLS_1_2] as an enabled protocol if called on an [SSLSocket]
     *
     * @return the (potentially modified) [Socket]
     */
    private fun Socket.patchForTls12(): Socket {
        return (this as? SSLSocket)?.apply {
            enabledProtocols += TlsVersion.TLS_1_2.javaName
        } ?: this
    }

    override fun getDefaultCipherSuites(): Array<String> {
        return delegate.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return delegate.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket? {
        return delegate.createSocket(s, host, port, autoClose)
            .patchForTls12()
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int): Socket? {
        return delegate.createSocket(host, port)
            .patchForTls12()
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int
    ): Socket? {
        return delegate.createSocket(host, port, localHost, localPort)
            .patchForTls12()
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket? {
        return delegate.createSocket(host, port)
            .patchForTls12()
    }

    @Throws(IOException::class)
    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int
    ): Socket? {
        return delegate.createSocket(address, port, localAddress, localPort)
            .patchForTls12()
    }
}

/**
 * @return [X509TrustManager] from [TrustManagerFactory]
 *
 * @throws [NoSuchElementException] if not found. According to the Android docs for [TrustManagerFactory], this
 * should never happen because PKIX is the only supported algorithm
 */
val trustManager by lazy {
    val trustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(null as KeyStore?)
    trustManagerFactory.trustManagers
        .first { it is X509TrustManager } as X509TrustManager
}

/**
 * If on [Build.VERSION_CODES.LOLLIPOP] or lower, sets [OkHttpClient.Builder.sslSocketFactory] to an instance of
 * [Tls12SocketFactory] that wraps the default [SSLContext.getSocketFactory] for [TlsVersion.TLS_1_2].
 *
 * Does nothing when called on [Build.VERSION_CODES.LOLLIPOP_MR1] or higher.
 *
 * For some reason, Android supports TLS v1.2 from [Build.VERSION_CODES.JELLY_BEAN], but the spec only has it
 * enabled by default from API [Build.VERSION_CODES.KITKAT]. Furthermore, some devices on
 * [Build.VERSION_CODES.LOLLIPOP] don't have it enabled, despite the spec saying they should.
 *
 * @return the (potentially modified) [OkHttpClient.Builder]
 */
fun OkHttpClient.Builder.enableTls12(): OkHttpClient.Builder = apply {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
        try {
            val sslContext = SSLContext.getInstance(TlsVersion.TLS_1_2.javaName)
            sslContext.init(null, arrayOf(trustManager), null)

            sslSocketFactory(
                Tls12SocketFactory(sslContext.socketFactory),
                trustManager
            )
        } catch (e: Exception) {
            Log.e("Tls12SocketFactory", "Error while setting TLS 1.2 compatibility", e)
        }
    }
}