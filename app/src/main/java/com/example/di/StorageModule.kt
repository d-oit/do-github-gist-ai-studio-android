package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.dao.GistDao

object StorageModule {

  fun provideAppDatabase(context: Context): AppDatabase {
    return Room.databaseBuilder(context, AppDatabase::class.java, "gist_database")
      .fallbackToDestructiveMigration(dropAllTables = true)
      .build()
  }

  fun provideGistDao(database: AppDatabase): GistDao {
    return database.gistDao()
  }
}
