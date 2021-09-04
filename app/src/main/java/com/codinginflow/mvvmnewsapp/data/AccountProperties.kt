package com.codinginflow.mvvmnewsapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_properties")
data class AccountProperties(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val token: String
)