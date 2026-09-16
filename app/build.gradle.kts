plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.ailivebear.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ailivebear.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-stage1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ---- Jetpack Compose (Settings/UI only - see main-screen rules in ui/MainScreen.kt) ----
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ---- Filament: real-time 3D rendering engine (see character/engine/) ----
    implementation("com.google.android.filament:filament-android:1.76.1")
    implementation("com.google.android.filament:gltfio-android:1.76.1")

    // ---- Local settings persistence (see settings/SettingsRepository.kt) ----
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ---- Coroutines (mock TikTok provider, settings flows) ----
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
