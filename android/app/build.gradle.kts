plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

import java.util.Properties

// Release signing is optional: without android/keystore.properties the project still
// configures and debug builds work, while release output falls back to the debug key.
val keystorePropsFile = rootProject.file("keystore.properties")
val hasReleaseKeystore = keystorePropsFile.exists()
val keystoreProps = Properties().apply {
    if (hasReleaseKeystore) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "work.bavshield.vpn"
    compileSdk = 35

    defaultConfig {
        applicationId = "work.bavshield.vpn"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "2.0.0"

        buildConfigField("String", "SITE_URL", "\"https://cabinet.bavshield.work/\"")
        buildConfigField("String", "TELEGRAM_BOT_URL", "\"https://t.me/BAVSVPN_bot\"")
        buildConfigField("String", "TELEGRAM_CHANNEL_URL", "\"https://t.me/BAVSVPN_bot\"")
        buildConfigField("String", "SUPPORT_URL", "\"https://t.me/EseCuloMexico\"")
        buildConfigField("String", "SUPPORT_EMAIL", "\"Bavshieldvpn@gmail.com\"")
        buildConfigField("String", "COPYRIGHT", "\"\\u00A9 Bavshield\"")

        // Payment destinations are placeholders until the final endpoints are provided.
        buildConfigField("String", "PAY_SUBSCRIPTION_URL", "\"https://t.me/BAVSVPN_bot\"")
        buildConfigField("String", "PAY_DEVICES_URL", "\"https://t.me/BAVSVPN_bot\"")
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                val storePath = checkNotNull(keystoreProps.getProperty("storeFile")) { "storeFile is missing" }
                storeFile = rootProject.file(storePath)
                storePassword = checkNotNull(keystoreProps.getProperty("storePassword")) { "storePassword is missing" }
                keyAlias = checkNotNull(keystoreProps.getProperty("keyAlias")) { "keyAlias is missing" }
                keyPassword = checkNotNull(keystoreProps.getProperty("keyPassword")) { "keyPassword is missing" }
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "BAV Shield: android/keystore.properties not found — the release build will be " +
                        "signed with the debug key and must not be published.",
                )
                signingConfigs.getByName("debug")
            }
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
    implementation("androidx.core:core-splashscreen:1.0.1")
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
