package com.uszkaisandor.mvvmnewsapp.shared

import com.uszkaisandor.mvvmnewsapp.api.AuthInterceptor
import com.uszkaisandor.mvvmnewsapp.data.AccountProperties

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