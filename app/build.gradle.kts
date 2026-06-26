plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.bibleread.bread"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.bibleread.bread"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 full mode: shrinks, obfuscates, and optimizes DEX
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled   = false
            isShrinkResources = false
        }
    }

    // Split one APK per ABI instead of bundling all native libs together.
    // Halves install size on most devices. Use AAB on Play Store instead for
    // automatic per-device delivery.
    splits {
        abi {
            isEnable          = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk   = false   // set true if you need a single fat APK for sideloading
        }
    }

    // Strip debug symbols from release native libs
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module",
                "kotlin/**",
                "DebugProbesKt.bin"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    // Do NOT compress already-compressed or directly-mapped file types.
    // .db must be uncompressed so Room can mmap it directly from the APK.
    androidResources {
        noCompress += listOf("db", "mp4", "webp", "png", "jpg", "ttf", "otf")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    // UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Image loading — SVG decoder removed (unused)
    implementation(libs.coil.compose)

    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Debug only
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
