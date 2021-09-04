package com.codinginflow.mvvmnewsapp.shared

import com.codinginflow.mvvmnewsapp.api.AuthInterceptor
import com.codinginflow.mvvmnewsapp.data.AccountProperties

class SessionManager
constructor(
    private val authInterceptor: AuthInterceptor
) {

    private var cachedAccountProperties: AccountProperties? = null

    fun setAccountProperties(accountProperties: AccountProperties?) {
        authInterceptor.token = accountProperties?.token ?: ""
        cachedAccountProperties = accountProperties
    }
}