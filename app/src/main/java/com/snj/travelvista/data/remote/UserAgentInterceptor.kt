package com.snj.travelvista.data.remote

import com.snj.travelvista.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", BuildConfig.API_USER_AGENT)
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}
