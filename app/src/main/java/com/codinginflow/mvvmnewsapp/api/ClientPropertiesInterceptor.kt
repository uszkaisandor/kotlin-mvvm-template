package com.codinginflow.mvvmnewsapp.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Header entry, which contains header name, header value and a boolean flag. The boolean
 * controls if it is a one-shot header entry.
 *
 * For example if values are <<"Accepted-Content-Language", "de", false>>
 * the header will contains the "Accepted-Content-Language" header entry with "de" value persistent
 * after the settings.
 *
 * But if values are <<"Accepted-Content-Language", "de", true>>
 * that means it is a one-shot event. Header will contains "Accepted-Content-Language" header entry
 * with "de" value. After request, header entry with "Accepted-Content-Language"
 * key will be removed from header entries.
 */
typealias HeaderEntry = Pair<Pair<String, String>, Boolean>

/**
 * Map of [HeaderEntry]s. The key is the name of the [HeaderEntry]. That will be guaranteed that
 * every header entry will be unique.
 */
typealias HeaderEntryMap = MutableMap<String, HeaderEntry>

class ClientPropertiesInterceptor : Interceptor {

    // Map of dynamic HeaderEntries
    private val headerEntries: HeaderEntryMap = mutableMapOf()

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val requestBuilder = request.newBuilder()

        // Add constant value to header. This will be added to all request
        requestBuilder.addHeader("platform", "android")

        // Add dynamic header entries
        headerEntries.forEach { headerEntry ->
            // Extract values from mapEntry
            requestBuilder.addHeader(
                headerEntry.value.getHeaderName(),
                headerEntry.value.getHeaderValue()
            )

            //Check if header entry is one-shot
            if (headerEntry.value.isOneShotHeader()) {
                // If it is one-shot, remove from the list
                headerEntries.remove(headerEntry.key)
            }
        }

        request = requestBuilder.build()

        return chain.proceed(request)
    }

    fun addHeaderEntries(vararg entries: HeaderEntry) {
        entries.forEach { entry ->
            this.headerEntries[entry.first.first] = entry
        }
    }
}

/**
 * Extract header name from [HeaderEntry]
 *
 * For example if values are <<"Accepted-Content-Language", "de", false>>
 * returns: Accepted-Content-Language
 */
fun HeaderEntry.getHeaderName() = first.first

/**
 * Extract header value from [HeaderEntry]
 *
 * For example if values are <<"Accepted-Content-Language", "de", false>>
 * returns: de
 */
fun HeaderEntry.getHeaderValue() = first.second

/**
 * Extract if [HeaderEntry] is one-shot
 *
 * For example if values are <<"Accepted-Content-Language", "de", false>>
 * returns: false
 */
fun HeaderEntry.isOneShotHeader() = second
