package com.chatapp.data.local

import androidx.room.*
import com.chatapp.data.model.GroupKey

@Dao
interface GroupKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupKey(groupKey: GroupKey)

    @Query("SELECT * FROM group_keys WHERE groupId = :groupId ORDER BY keyVersion DESC LIMIT 1")
    suspend fun getLatestGroupKey(groupId: String): GroupKey?
}
