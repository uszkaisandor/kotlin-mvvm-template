package com.uszkaisandor.mvvmnewsapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AccountPropertiesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccountProperties(accountProperties: AccountProperties)

    @Query("SELECT * FROM account_properties WHERE id = 1")
    fun getAccountProperties(): AccountProperties?
}