package fr.frankois944.ktorfilecaching

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ApiTest {
    private val caching = KtorFileCaching(storedCacheDirectory = "testCache".toPath(), fileSystem = FakeFileSystem())

    @ExperimentalCoroutinesApi
    @BeforeTest
    fun beforeTest() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher())
            caching.purgeCache()
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
                        headersOf(
                            Pair(
                                HttpHeaders.ContentType,
                                listOf("application/json"),
                            ),
                            Pair(
                                HttpHeaders.Date,
                                listOf(
                                    Clock.System.now().toString(),
                                ),
                            ),
                        ),
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
    fun testSimpleKtorCache() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content =
                            ByteReadChannel(
                                """
                                {"ip":"127.0.0.1", "time" : ${Clock.System.now()}}
                                """.trimIndent(),
                            ),
                        status = HttpStatusCode.OK,
                        headers =
                            headersOf(
                                Pair(
                                    HttpHeaders.ContentType,
                                    listOf("application/json"),
                                ),
                                Pair(
                                    HttpHeaders.Date,
                                    listOf(Clock.System.now().toString()),
                                ),
                                Pair(
                                    HttpHeaders.CacheControl,
                                    listOf("max-age=600"),
                                ),
                            ),
                    )
                }

            val client =
                ApiClient(
                    HttpClient(mockEngine) {
                        install(HttpCache) {
                            publicStorage(caching)
                        }
                        install(Logging) {
                            logger =
                                object : Logger {
                                    override fun log(message: String) {
                                        println(message)
                                    }
                                }
                            level = LogLevel.ALL
                        }
                    },
                )

            val firstResponse = client.getIp()
            delay(500)
            val cachedResponse = client.getIp()
            assertEquals(firstResponse.bodyAsText(), cachedResponse.bodyAsText())
            assertEquals(firstResponse.headers, cachedResponse.headers)
            assertEquals(firstResponse.status, cachedResponse.status)
            client.httpClient.close()
        }

    @Test
    fun testComplexKtorCache() =
        runTest {
            var index = 0
            // build a list of request with 1 param with body
            // Pair<String, ByteReadChannel>, for 1 parameter and the body content
            val iteration =
                buildList {
                    repeat(6) {
                        add(
                            Pair(
                                "Request_param__${index++}",
                                ByteReadChannel(
                                    """
                                    {"ip":"127.0.0.1", "time" : ${Clock.System.now()}, "data" : ${Clock.System.now().nanosecondsOfSecond}}
                                    """.trimIndent(),
                                ),
                            ),
                        )
                    }
                }
            // build a big list of randomized request from the original list of request
            val bodies = (iteration + iteration + iteration).shuffled()
            bodies.forEach { body ->
                val mockEngine =
                    MockEngine {
                        respond(
                            content = body.second,
                            status = HttpStatusCode.OK,
                            headers =
                                headersOf(
                                    Pair(
                                        HttpHeaders.ContentType,
                                        listOf("application/json"),
                                    ),
                                    Pair(
                                        HttpHeaders.Date,
                                        listOf(Clock.System.now().toString()),
                                    ),
                                    Pair(
                                        HttpHeaders.CacheControl,
                                        listOf("max-age=600"),
                                    ),
                                ),
                        )
                    }

                val client =
                    ApiClient(
                        HttpClient(mockEngine) {
                            install(HttpCache) {
                                publicStorage(caching)
                            }
                            install(Logging) {
                                logger =
                                    object : Logger {
                                        override fun log(message: String) {
                                            println(message)
                                        }
                                    }
                                level = LogLevel.ALL
                            }
                        },
                    )

                val firstResponse = client.getIpWithParam(body.first)
                delay(500)
                val cachedResponse = client.getIpWithParam(body.first)
                assertEquals(firstResponse.bodyAsText(), cachedResponse.bodyAsText())
                assertEquals(firstResponse.headers, cachedResponse.headers)
                assertEquals(firstResponse.status, cachedResponse.status)
            }
        }

    @Test
    fun testWithoutKtorCache() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content =
                            ByteReadChannel(
                                """
                                {"ip":"127.0.0.1", "time" : ${Clock.System.now()}}
                                """.trimIndent(),
                            ),
                        status = HttpStatusCode.OK,
                        headers =
                            headersOf(
                                Pair(
                                    HttpHeaders.ContentType,
                                    listOf("application/json"),
                                ),
                                Pair(
                                    HttpHeaders.Date,
                                    listOf(Clock.System.now().toString()),
                                ),
                                Pair(
                                    HttpHeaders.CacheControl,
                                    listOf("max-age=600"),
                                ),
                            ),
                    )
                }

            val client =
                ApiClient(
                    HttpClient(mockEngine) {
                        install(Logging) { level = LogLevel.INFO }
                    },
                )

            val firstResponse = client.getIp()
            delay(750)
            val cachedResponse = client.getIp()
            assertNotEquals(firstResponse.bodyAsText(), cachedResponse.bodyAsText())
            assertNotEquals(firstResponse.headers, cachedResponse.headers)
            assertEquals(firstResponse.status, cachedResponse.status)
            client.httpClient.close()
        }
}
