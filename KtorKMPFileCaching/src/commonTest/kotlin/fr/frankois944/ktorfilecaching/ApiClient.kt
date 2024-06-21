package fr.frankois944.ktorfilecaching

import io.ktor.client.HttpClient
import io.ktor.client.request.get

class ApiClient(
    val httpClient: HttpClient,
) {
    suspend fun getIp() = httpClient.get("https://api.ipify.org/?format=json")
}
