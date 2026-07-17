import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
  alias(libs.plugins.spotless)
  alias(libs.plugins.detekt)
  jacoco
}

jacoco { toolVersion = "0.8.12" }

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }
  buildToolsVersion = "35.0.0"

  defaultConfig {
    applicationId = "com.aistudio.dogisthub.qwerty"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
  }

  signingConfigs {
    // Release signing is completely optional/opt-in via Gradle properties or environment variables
    val releaseStoreFile =
      project.findProperty("RELEASE_STORE_FILE") as? String ?: System.getenv("RELEASE_STORE_FILE")
    val releaseStorePassword =
      project.findProperty("RELEASE_STORE_PASSWORD") as? String
        ?: System.getenv("RELEASE_STORE_PASSWORD")
    val releaseKeyAlias =
      project.findProperty("RELEASE_KEY_ALIAS") as? String ?: System.getenv("RELEASE_KEY_ALIAS")
    val releaseKeyPassword =
      project.findProperty("RELEASE_KEY_PASSWORD") as? String
        ?: System.getenv("RELEASE_KEY_PASSWORD")

    val hasReleaseSigning =
      !releaseStoreFile.isNullOrEmpty() &&
        !releaseStorePassword.isNullOrEmpty() &&
        !releaseKeyAlias.isNullOrEmpty() &&
        !releaseKeyPassword.isNullOrEmpty()

    if (hasReleaseSigning) {
      create("release") {
        storeFile = file(releaseStoreFile!!)
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }

    // Default debug signing uses built-in "debug" signing config.
    // If rootDir's debug.keystore exists, map it; otherwise let standard Gradle defaults apply.
    getByName("debug") {
      val localDebugKeystore = file("${rootDir}/debug.keystore")
      if (localDebugKeystore.exists()) {
        storeFile = localDebugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

      val releaseStoreFile =
        project.findProperty("RELEASE_STORE_FILE") as? String ?: System.getenv("RELEASE_STORE_FILE")
      val releaseStorePassword =
        project.findProperty("RELEASE_STORE_PASSWORD") as? String
          ?: System.getenv("RELEASE_STORE_PASSWORD")
      val releaseKeyAlias =
        project.findProperty("RELEASE_KEY_ALIAS") as? String ?: System.getenv("RELEASE_KEY_ALIAS")
      val releaseKeyPassword =
        project.findProperty("RELEASE_KEY_PASSWORD") as? String
          ?: System.getenv("RELEASE_KEY_PASSWORD")

      val hasReleaseSigning =
        !releaseStoreFile.isNullOrEmpty() &&
          !releaseStorePassword.isNullOrEmpty() &&
          !releaseKeyAlias.isNullOrEmpty() &&
          !releaseKeyPassword.isNullOrEmpty()

      if (hasReleaseSigning) {
        signingConfig = signingConfigs.getByName("release")
      } else {
        // Dynamic check: throw error only when release task is explicitly run without credentials
        gradle.taskGraph.whenReady {
          if (hasTask(":app:assembleRelease") || hasTask(":app:bundleRelease")) {
            throw GradleException(
              "Release signing credentials missing. Please define RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, and RELEASE_KEY_PASSWORD."
            )
          }
        }
      }
    }
    debug { signingConfig = signingConfigs.getByName("debug") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }

  lint {
    abortOnError = true
    xmlReport = true
    htmlReport = true
    textReport = true
    textOutput = file("stdout")
    fatal += "all" // promote all fatal lints to errors
  }
}

tasks.register<JacocoReport>("jacocoTestReportDebug") {
  dependsOn("testDebugUnitTest")
  group = "Reporting"
  description = "Generate JaCoCo coverage report for debug unit tests."

  reports {
    xml.required.set(true)
    html.required.set(true)
    csv.required.set(false)
  }

  val excludes =
    listOf(
      "**/R.class",
      "**/R$*.class",
      "**/BuildConfig.*",
      "**/Manifest*.*",
      "**/*Test*.*",
      "android/**/*.*",
      "**/databinding/**",
      "**/generated/**",
      "**/*_MembersInjector*.*",
      "**/*Dagger*.*",
      "**/*_Factory*.*",
    )

  // AGP places Kotlin compiled classes here for unit tests:
  val kotlinClasses =
    fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") { exclude(excludes) }

  sourceDirectories.setFrom(files("${project.projectDir}/src/main/java"))
  classDirectories.setFrom(files(kotlinClasses))

  // Execution data path produced by AGP for debug unit tests:
  executionData.setFrom(
    fileTree(layout.buildDirectory.get()) {
      include(
        "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
        "jacoco/testDebugUnitTest.exec",
      )
    }
  )
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("CODACY_PROJECT_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
  // implementation(libs.hilt.android)
  // "ksp"(libs.hilt.compiler)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.security.crypto)
}

spotless {
  kotlin {
    target("**/*.kt")
    targetExclude("**/build/**/*.kt")
    ktfmt().googleStyle()
  }
  kotlinGradle {
    target("**/*.gradle.kts")
    ktfmt().googleStyle()
  }
}

detekt {
  toolVersion = libs.versions.detekt.get()
  config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
  buildUponDefaultConfig = true
  allRules = false
  disableDefaultRuleSets = false
  parallel = true
}
