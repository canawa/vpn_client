plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

import java.util.Properties

android {
    namespace = "ru.coffeemaniavpn.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.coffeemaniavpn.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "SUBSCRIPTION_STORE_URL", "\"https://coffemaniavpn.online\"")
        buildConfigField("String", "SUBSCRIPTION_REGISTER_URL", "\"https://coffeemaniavpn.ru/register\"")
        buildConfigField("String", "TELEGRAM_CHANNEL_URL", "\"https://t.me/coffemaniavpn\"")
        buildConfigField("String", "TELEGRAM_BOT_URL", "\"https://t.me/coffemaniavpnbot\"")
    }

    signingConfigs {
        create("release") {
            val keystorePropsFile = rootProject.file("keystore.properties")
            require(keystorePropsFile.exists()) {
                "Missing android/keystore.properties — copy keystore.properties.example and fill in upload key credentials."
            }
            val keystoreProps = Properties()
            keystorePropsFile.inputStream().use { keystoreProps.load(it) }
            val storePath = checkNotNull(keystoreProps.getProperty("storeFile")) { "storeFile is missing" }
            storeFile = rootProject.file(storePath)
            storePassword = checkNotNull(keystoreProps.getProperty("storePassword")) { "storePassword is missing" }
            keyAlias = checkNotNull(keystoreProps.getProperty("keyAlias")) { "keyAlias is missing" }
            keyPassword = checkNotNull(keystoreProps.getProperty("keyPassword")) { "keyPassword is missing" }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("adiVerify") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
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
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(files("libs/libv2ray.aar"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
