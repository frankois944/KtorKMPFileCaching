package fr.frankois944.ktorfilecaching

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonGreetingTest {
    @ExperimentalCoroutinesApi
    @BeforeTest
    fun beforeTest() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @ExperimentalCoroutinesApi
    @AfterTest
    fun afterTest() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSerializeCachedResponseData() {
        val originalResponseData =
            SerializableCachedResponseData(
                CachedResponseData(
                    url = Url("https://www.google.com"),
                    statusCode = HttpStatusCode.Accepted,
                    requestTime = GMTDate.START,
                    responseTime = GMTDate.START,
                    version = HttpProtocolVersion.HTTP_1_1,
                    expires = GMTDate.START,
                    headers =
                        Headers.build {
                            this["Content-Type"] = "application/json"
                        },
                    varyKeys = emptyMap(),
                    body = "laurem ipsum".encodeToByteArray(),
                ),
            )

        val encodedResponseData = Json.encodeToString(originalResponseData)
        val decodedResponseData = Json.decodeFromString<SerializableCachedResponseData>(encodedResponseData)
        assertEquals(originalResponseData, decodedResponseData)
        assertEquals(originalResponseData.cachedResponseData, decodedResponseData.cachedResponseData)
    }

    @Test
    fun testKtorCache() {
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = ByteReadChannel("""{"ip":"127.0.0.1"}"""),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client =
                ApiClient(
                    HttpClient(mockEngine) {
                        install(HttpCache) {
                            publicStorage(KtorFileCaching(FakeFileSystem()))
                        }
                        install(Logging) { level = LogLevel.INFO }
                    },
                )

            val firstResponse = client.getIp()
            val cachedResponse = client.getIp()
            assertEquals(firstResponse.bodyAsText(), cachedResponse.bodyAsText())
            assertEquals(firstResponse.headers, cachedResponse.headers)
            assertEquals(firstResponse.status, cachedResponse.status)
            client.httpClient.close()
        }
    }
}
