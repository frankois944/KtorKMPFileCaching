@file:OptIn(ExperimentalTime::class)

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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class FakeClock(
    var currentTime: Instant,
) : Clock {
    override fun now(): Instant = currentTime

    fun advanceBy(duration: Duration) {
        currentTime += duration
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ApiTest {
    private val filesystem = FakeFileSystem()
    private val clock = FakeClock(Clock.System.now())

    private val caching =
        KtorFileCaching(
            storedCacheDirectory = "testCache".toPath(),
            fileSystem = filesystem,
        )

    @ExperimentalCoroutinesApi
    @BeforeTest
    fun beforeTest() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher())
            filesystem.deleteRecursively("testCache".toPath())
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
                                    clock.now().toString(),
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
                                {"ip":"127.0.0.1", "time" : ${clock.now()}}
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
                                    listOf(clock.now().toString()),
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
            clock.advanceBy(3.seconds)
            val cachedResponse = client.getIp()
            assertEquals(firstResponse.bodyAsText(), cachedResponse.bodyAsText())
            assertEquals(firstResponse.headers, cachedResponse.headers)
            assertEquals(firstResponse.status, cachedResponse.status)
            client.httpClient.close()
        }

    @Test
    fun testComplexKtorCache() =
        runTest {
            // build a list of request with 1 param with body
            // Pair<String, ByteReadChannel>, for 1 parameter and the body content
            val iteration =
                buildList {
                    repeat(6) { index ->
                        add(
                            Pair(
                                "Request_param__$index",
                                ByteReadChannel(
                                    """
                                    {"ip":"127.0.0.1", "time" : ${clock.now()}, "data" : ${clock.now().epochSeconds}.${clock.now().nanosecondsOfSecond}}
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
                                        listOf(clock.now().toString()),
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
                clock.advanceBy(3.seconds)
                client.getIpWithParam(body.first)
            }
            val logs = mutableListOf<String>()
            bodies.shuffled().forEach { body ->
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
                                        listOf(clock.now().toString()),
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
                                            logs.add(message)
                                        }
                                    }
                                level = LogLevel.ALL
                            }
                        },
                    )

                val response = client.getIpWithParam(body.first)
                assertTrue(response.bodyAsText().isNotEmpty(), "The cached request must have a response body")
            }

            assertTrue(logs.isEmpty(), "No log should be generated when getting cached data")
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
                                {"ip":"127.0.0.1", "time" : ${clock.now()}, "nanosecond" : ${clock.now().nanosecondsOfSecond}}
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
                                    listOf(clock.now().toString()),
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
            clock.advanceBy(3.seconds)
            val cachedResponse = client.getIp()
            assertNotEquals(firstResponse.bodyAsText(), cachedResponse.bodyAsText())
            assertNotEquals(firstResponse.headers, cachedResponse.headers)
            assertEquals(firstResponse.status, cachedResponse.status)
            client.httpClient.close()
        }

    @Test
    fun testRemoveOne() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content =
                            ByteReadChannel(
                                """
                                {"ip":"127.0.0.1", "time" : ${clock.now()}}
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
                                    listOf(clock.now().toString()),
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
            caching.remove(url = Url("https://api.ipify.org/?format=json"), varyKeys = emptyMap())
            clock.advanceBy(3.seconds)
            val cachedResponse = client.getIp()
            assertNotEquals(firstResponse.bodyAsText(), cachedResponse.bodyAsText())
            assertNotEquals(firstResponse.headers, cachedResponse.headers)
            assertEquals(firstResponse.status, cachedResponse.status)
            client.httpClient.close()
        }

    @Test
    fun testRemoveAll() =
        runTest {
            // build a list of request with 1 param with body
            // Pair<String, ByteReadChannel>, for 1 parameter and the body content
            val iteration =
                buildList {
                    repeat(6) { index ->
                        add(
                            Pair(
                                "Request_param__$index",
                                """
                                {"ip":"127.0.0.1", "time" : ${clock.now()}, "data" : ${clock.now().epochSeconds}.${clock.now().nanosecondsOfSecond}}
                                """.trimIndent(),
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
                                        listOf(clock.now().toString()),
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
                clock.advanceBy(3.seconds)
                client.getIpWithParam(body.first)
            }
            clock.advanceBy(3.seconds)
            repeat(6) {
                caching.removeAll(url = Url("https://api.ipify.org/?format=json&param=Request_param__$it"))
            }
            val logs = mutableListOf<String>()
            bodies.shuffled().forEach { body ->
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
                                        listOf(clock.now().toString()),
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
                                            logs.add(message)
                                        }
                                    }
                                level = LogLevel.ALL
                            }
                        },
                    )

                val response = client.getIpWithParam(body.first)
                assertTrue(response.bodyAsText().isNotEmpty(), "The cached request must have a response body")
            }

            assertFalse(logs.isEmpty(), "The logs should be generated when getting cached data because of the removal")
        }
}
