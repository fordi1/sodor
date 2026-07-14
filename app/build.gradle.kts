import java.net.URL
import java.io.FileOutputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.example"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  packagingOptions {
    resources.excludes.add("META-INF/DEPENDENCIES")
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

tasks.register("downloadFonts") {
  notCompatibleWithConfigurationCache("Downloads fonts at configuration/initialization stage")
  doLast {
    val fontsDir = file("src/main/assets/fonts")
    if (!fontsDir.exists()) {
      fontsDir.mkdirs()
    }
    val regularFont = file("src/main/assets/fonts/vazirmatn_regular.ttf")
    val boldFont = file("src/main/assets/fonts/vazirmatn_bold.ttf")
    
    if (!regularFont.exists()) {
      println("Downloading Vazirmatn Regular...")
      try {
        val url = URL("https://github.com/rastikerdar/vazirmatn/raw/master/fonts/ttf/Vazirmatn-Regular.ttf")
        val stream = url.openStream()
        val outStream = FileOutputStream(regularFont)
        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (stream.read(buffer).also { bytesRead = it } != -1) {
          outStream.write(buffer, 0, bytesRead)
        }
        outStream.close()
        stream.close()
        println("Vazirmatn Regular downloaded successfully!")
      } catch (e: Exception) {
        println("Failed to download Vazirmatn Regular: ${e.message}")
      }
    }
    if (!boldFont.exists()) {
      println("Downloading Vazirmatn Bold...")
      try {
        val url = URL("https://github.com/rastikerdar/vazirmatn/raw/master/fonts/ttf/Vazirmatn-Bold.ttf")
        val stream = url.openStream()
        val outStream = FileOutputStream(boldFont)
        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (stream.read(buffer).also { bytesRead = it } != -1) {
          outStream.write(buffer, 0, bytesRead)
        }
        outStream.close()
        stream.close()
        println("Vazirmatn Bold downloaded successfully!")
      } catch (e: Exception) {
        println("Failed to download Vazirmatn Bold: ${e.message}")
      }
    }
  }
}

tasks.named("preBuild") {
  dependsOn("downloadFonts")
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(libs.play.services.auth)
  implementation(libs.google.api.client.android)
  implementation(libs.google.api.services.drive)
  implementation(libs.google.api.client.gson)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)
  implementation(libs.gson)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.play.services.codescanner)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.mlkit.barcode.scanning)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
