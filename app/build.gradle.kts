import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Load keystore properties
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) load(file.inputStream())
}

android {
    namespace = "ai.joshmiller.orrery"
    compileSdk = 35

    defaultConfig {
        applicationId = "ai.joshmiller.orrery"
        minSdk = 30
        targetSdk = 35
        versionCode = 5
        versionName = "1.1.1"
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile", ""))
            storePassword = keystoreProperties.getProperty("storePassword", "")
            keyAlias = keystoreProperties.getProperty("keyAlias", "")
            keyPassword = keystoreProperties.getProperty("keyPassword", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2026.03.00")
    implementation(composeBom)

    // Wear OS Compose
    implementation("androidx.wear.compose:compose-material3:1.6.0")
    implementation("androidx.wear.compose:compose-foundation:1.6.0")

    // Core Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.9.0")
    // The watchface libraries pull in fragment 1.1.0 transitively, which is
    // too old for the ActivityResult APIs — pin a modern version
    implementation("androidx.fragment:fragment:1.7.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Astronomy Engine — offline planetary position calculations
    implementation("com.github.cosinekitty:astronomy:v2.1.17")

    // Google Play Services — Location (GPS)
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Watch face complications
    implementation("androidx.wear.watchface:watchface-complications-data-source-ktx:1.2.0")

    // Protolayout — pinned to fix CVE-2024-7254
    implementation("androidx.wear.protolayout:protolayout-proto:1.2.1")
    implementation("androidx.wear.protolayout:protolayout-external-protobuf:1.2.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")
}
