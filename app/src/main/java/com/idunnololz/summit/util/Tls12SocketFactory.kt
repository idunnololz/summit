package com.idunnololz.summit.util

import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.security.KeyStore
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import okhttp3.TlsVersion

/**
 * Implementation of [SSLSocketFactory] that adds [TlsVersion.TLS_1_2] as an enabled protocol for every [SSLSocket]
 * created by [delegate].
 *
 * [See this discussion for more details.](https://github.com/square/okhttp/issues/2372#issuecomment-244807676)
 *
 */
class Tls12SocketFactory(
  private val delegate: SSLSocketFactory,
) : SSLSocketFactory() {

  /**
   * Forcefully adds [TlsVersion.TLS_1_2] as an enabled protocol if called on an [SSLSocket]
   *
   * @return the (potentially modified) [Socket]
   */
  private fun Socket.patchForTls12(): Socket = (this as? SSLSocket)?.apply {
    enabledProtocols += TlsVersion.TLS_1_2.javaName
  } ?: this

  override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites

  override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

  @Throws(IOException::class)
  override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket? =
    delegate.createSocket(s, host, port, autoClose)
      .patchForTls12()

  @Throws(IOException::class, UnknownHostException::class)
  override fun createSocket(host: String, port: Int): Socket? = delegate.createSocket(host, port)
    .patchForTls12()

  @Throws(IOException::class, UnknownHostException::class)
  override fun createSocket(
    host: String,
    port: Int,
    localHost: InetAddress,
    localPort: Int,
  ): Socket? = delegate.createSocket(host, port, localHost, localPort)
    .patchForTls12()

  @Throws(IOException::class)
  override fun createSocket(host: InetAddress, port: Int): Socket? =
    delegate.createSocket(host, port)
      .patchForTls12()

  @Throws(IOException::class)
  override fun createSocket(
    address: InetAddress,
    port: Int,
    localAddress: InetAddress,
    localPort: Int,
  ): Socket? = delegate.createSocket(address, port, localAddress, localPort)
    .patchForTls12()
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
