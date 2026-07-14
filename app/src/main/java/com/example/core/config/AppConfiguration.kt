package com.example.core.config

interface AppConfiguration {
  fun geminiApiKeyOrNull(): String?
}

class AppConfigurationImpl : AppConfiguration {
  override fun geminiApiKeyOrNull(): String? {
    return try {
      val field = Class.forName("com.example.BuildConfig").getField("GEMINI_API_KEY")
      val key = field.get(null) as? String
      if (key.isNullOrBlank() || key == "MY_GEMINI_API_KEY") null else key
    } catch (e: Exception) {
      null
    }
  }
}
