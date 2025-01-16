package fr.frankois944.ktorfilecaching

import io.ktor.client.HttpClient
import io.ktor.client.request.*

class ApiClient(
    val httpClient: HttpClient,
) {
    suspend fun getIp() = httpClient.get("https://api.ipify.org/?format=json")

    suspend fun getIpWithParam(param: String) = httpClient.get("https://api.ipify.org/?format=json&param=$param")
}
