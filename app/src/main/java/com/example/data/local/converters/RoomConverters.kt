package com.example.data.local.converters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class RoomConverters {
  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
  private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

  @TypeConverter
  fun fromStringList(value: List<String>?): String? {
    return value?.let { stringListAdapter.toJson(it) }
  }

  @TypeConverter
  fun toStringList(value: String?): List<String>? {
    return value?.let { stringListAdapter.fromJson(it) }
  }
}
