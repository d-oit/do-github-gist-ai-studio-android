package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.converters.RoomConverters
import com.example.data.local.dao.GistDao
import com.example.data.local.entity.GistEntity
import com.example.data.local.entity.GistFileEntity

@Database(
    entities = [
        GistEntity::class,
        GistFileEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gistDao(): GistDao
}
