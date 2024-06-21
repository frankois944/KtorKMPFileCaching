package fr.frankois944.ktorfilecaching

import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import io.ktor.util.toMap
import kotlinx.serialization.Serializable

@Serializable
public class SerializableCachedResponseData {
    private val url: String
    private val statusCode: Int
    private val requestTime: Long
    private val responseTime: Long
    private val version: String
    private val expires: Long
    private val headers: Map<String, List<String>>
    private val varyKeys: Map<String, String>
    private val body: ByteArray

    public constructor(source: CachedResponseData) {
        this.url = source.url.toString()
        this.statusCode = source.statusCode.value
        this.requestTime = source.requestTime.timestamp
        this.responseTime = source.responseTime.timestamp
        this.version = source.version.toString()
        this.expires = source.expires.timestamp
        this.headers = source.headers.toMap()
        this.varyKeys = source.varyKeys
        this.body = source.body
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SerializableCachedResponseData) return false

        if (url != other.url) return false
        if (varyKeys != other.varyKeys) return false
        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + varyKeys.hashCode()
        return result
    }

    public val cachedResponseData: CachedResponseData
        get() =
            CachedResponseData(
                url = Url(url),
                statusCode = HttpStatusCode.fromValue(statusCode),
                requestTime = GMTDate(requestTime),
                responseTime = GMTDate(responseTime),
                version = HttpProtocolVersion.parse(version),
                expires = GMTDate(expires),
                headers =
                    Headers.build {
                        headers.forEach { (key, value) ->
                            value.forEach { this[key] = it }
                        }
                    },
                varyKeys = varyKeys,
                body = body,
            )
}
