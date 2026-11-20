package com.snj.travelvista.data.remote

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.net.URLDecoder

class MockTravelInterceptor : Interceptor {

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val pathSegments = url.pathSegments

        val responseJson: String
        var statusCode = 200

        when {
            // GET /destinations
            pathSegments.isNotEmpty() && pathSegments[0] == "destinations" && pathSegments.size == 1 -> {
                val categoryParam = url.queryParameter("category")?.let { URLDecoder.decode(it, "UTF-8") }
                val searchParam = url.queryParameter("search")?.let { URLDecoder.decode(it, "UTF-8") }

                var list = TravelMockData.destinations

                if (!categoryParam.isNullOrBlank() && !categoryParam.equals("all", ignoreCase = true)) {
                    list = list.filter { it.category.equals(categoryParam, ignoreCase = true) }
                }

                if (!searchParam.isNullOrBlank()) {
                    val query = searchParam.trim().lowercase()
                    list = list.filter {
                        it.title.lowercase().contains(query) ||
                                it.country.lowercase().contains(query) ||
                                it.city.lowercase().contains(query) ||
                                it.category.lowercase().contains(query) ||
                                it.tags.any { tag -> tag.lowercase().contains(query) }
                    }
                }

                responseJson = gson.toJson(list)
            }

            // GET /destinations/{id}
            pathSegments.isNotEmpty() && pathSegments[0] == "destinations" && pathSegments.size >= 2 -> {
                val id = pathSegments[1].toLongOrNull()
                val destination = TravelMockData.destinations.find { it.id == id }
                if (destination != null) {
                    responseJson = gson.toJson(destination)
                } else {
                    statusCode = 404
                    responseJson = """{"error": "Destination not found"}"""
                }
            }

            // GET /categories
            pathSegments.isNotEmpty() && pathSegments[0] == "categories" -> {
                responseJson = gson.toJson(TravelMockData.categories)
            }

            else -> {
                // Return default list for anything else
                responseJson = gson.toJson(TravelMockData.destinations)
            }
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = responseJson.toResponseBody(mediaType)

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(statusCode)
            .message(if (statusCode == 200) "OK" else "Not Found")
            .body(body)
            .addHeader("content-type", "application/json")
            .build()
    }
}
