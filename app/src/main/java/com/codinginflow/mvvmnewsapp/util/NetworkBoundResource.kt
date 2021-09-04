package com.codinginflow.mvvmnewsapp.util

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

inline fun <CacheResult, NetworkResult> networkBoundResource(
    crossinline queryFromCache: () -> Flow<CacheResult>,
    crossinline fetchFromNetwork: suspend () -> NetworkResult,
    crossinline saveFetchResult: suspend (NetworkResult) -> Unit,
    crossinline shouldFetch: (CacheResult) -> Boolean = { true },
    crossinline onFetchSuccess: () -> Unit = { },
    crossinline onFetchFailed: (Throwable) -> Unit = { }
) = channelFlow {

    val data = queryFromCache().first()

    if (shouldFetch(data)) {
        val loading = launch {
            queryFromCache().collect {
                send(Resource.Loading(it))
            }
        }

        try {
            saveFetchResult(fetchFromNetwork())
            onFetchSuccess()
            loading.cancel()
            queryFromCache().collect {
                send(Resource.Success(it))
            }
        } catch (t: Throwable) {
            onFetchFailed(t)
            loading.cancel()
            queryFromCache().collect {
                send(Resource.Error(t, it))
            }
        }
    } else {
        queryFromCache().collect {
            send(Resource.Success(it))
        }
    }
}