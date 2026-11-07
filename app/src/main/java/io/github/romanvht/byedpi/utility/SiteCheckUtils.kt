package io.github.romanvht.byedpi.utility

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import kotlin.coroutines.coroutineContext

class SiteCheckUtils(
    private val proxyIp: String,
    private val proxyPort: Int
) {

    suspend fun checkSitesAsync(
        sites: List<String>,
        requestsCount: Int,
        requestTimeout: Long,
        concurrentRequests: Int = 20,
        fullLog: Boolean,
        onSiteChecked: (suspend (String, Int, Int) -> Unit)? = null
    ): List<Pair<String, Int>> {
        val semaphore = Semaphore(concurrentRequests)
        return withContext(Dispatchers.IO) {
            sites.map { site ->
                async {
                    semaphore.withPermit {
                        val successCount = checkSiteAccess(site, requestsCount, requestTimeout)
                        coroutineContext.ensureActive()
                        if (fullLog) {
                            onSiteChecked?.invoke(site, successCount, requestsCount)
                        }
                        site to successCount
                    }
                }
            }.awaitAll()
        }
    }

    private suspend fun checkSiteAccess(
        site: String,
        requestsCount: Int,
        timeout: Long
    ): Int = withContext(Dispatchers.IO) {
        var responseCount = 0

        val formattedUrl = if (site.startsWith("http://") || site.startsWith("https://")) site
        else "https://$site"

        val url = try {
            URL(formattedUrl)
        } catch (_: Exception) {
            Log.e("SiteChecker", "Invalid URL: $formattedUrl")
            return@withContext 0
        }

        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(proxyIp, proxyPort))

        repeat(requestsCount) { attempt ->
            coroutineContext.ensureActive()
            Log.i("SiteChecker", "Attempt ${attempt + 1}/$requestsCount for $site")

            var connection: HttpURLConnection? = null
            try {
                connection = url.openConnection(proxy) as HttpURLConnection
                connection.connectTimeout = (timeout * 1000).toInt()
                connection.readTimeout = (timeout * 1000).toInt()
                connection.instanceFollowRedirects = true
                connection.setRequestProperty("Connection", "close")

                val responseCode = connection.responseCode
                val declaredLength = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    connection.contentLengthLong
                } else {
                    connection.contentLength.toLong()
                }

                val inputStream = try {
                    if (responseCode in 200..299) connection.inputStream else connection.errorStream
                } catch (_: IOException) {
                    null
                }
                val body = readResponseBody(inputStream, declaredLength) { coroutineContext.ensureActive() }

                if (body.isComplete) {
                    Log.i("SiteChecker", "Response for $site: $responseCode, Declared: $declaredLength, Actual: ${body.length}")
                    responseCount++
                } else {
                    Log.w("SiteChecker", "Block detected for $site, Declared: $declaredLength, Actual: ${body.length}")
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SiteChecker", "Error accessing $site: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }

        responseCount
    }
}

/**
 * Body bytes read before the check is considered passed. DPI boxes that throttle by
 * volume usually freeze the connection after 16-24 KB, so this keeps a wide margin
 * without downloading whole pages.
 */
internal const val BODY_CHECK_LIMIT = 128L * 1024

internal data class BodyReadResult(val length: Long, val isComplete: Boolean)

/**
 * Reads up to [BODY_CHECK_LIMIT] bytes of a response body.
 *
 * The body counts as complete only when the stream ended normally with the declared
 * length (or any length if none was declared), or when the limit was reached.
 * A read error or timeout in the middle of the body is a failure: this is how
 * volume-based blocking looks for chunked and compressed responses that carry
 * no Content-Length.
 */
internal fun readResponseBody(
    input: InputStream?,
    declaredLength: Long,
    limit: Long = BODY_CHECK_LIMIT,
    ensureActive: () -> Unit = {},
): BodyReadResult {
    if (input == null) return BodyReadResult(0, true)

    val target = if (declaredLength > 0) minOf(declaredLength, limit) else limit
    val buffer = ByteArray(8192)
    var length = 0L
    try {
        input.use { stream ->
            while (length < target) {
                ensureActive()
                val toRead = minOf(buffer.size.toLong(), target - length).toInt()
                val read = stream.read(buffer, 0, toRead)
                if (read == -1) break
                length += read
            }
        }
    } catch (_: IOException) {
        return BodyReadResult(length, false)
    }

    val complete = length >= target || declaredLength <= 0
    return BodyReadResult(length, complete)
}
