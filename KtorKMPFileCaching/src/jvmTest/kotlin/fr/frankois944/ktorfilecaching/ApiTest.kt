@file:OptIn(ExperimentalTime::class)

package fr.frankois944.ktorfilecaching

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.client.plugins.cache.storage.FileStorage
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
import java.nio.file.Files
import java.nio.file.Paths
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

@OptIn(ExperimentalCoroutinesApi::class)
class ApiJVMTest {
    private val filesystem = FakeFileSystem()
    private val clock = FakeClock(Clock.System.now())

    val cacheFile = Files.createDirectories(Paths.get("/tmp/ktor/cache")).toFile()
    val caching = FileStorage(cacheFile)

    @ExperimentalCoroutinesApi
    @BeforeTest
    fun beforeTest() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher())
        }

    @ExperimentalCoroutinesApi
    @AfterTest
    fun afterTest() {
        Dispatchers.resetMain()
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
