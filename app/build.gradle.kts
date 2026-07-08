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
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // Reads from project properties if available (e.g. from local.properties or gradle.properties)
            val storeFilePath = project.findProperty("RELEASE_STORE_FILE")?.toString()
            val keystoreFile = if (storeFilePath != null) file(storeFilePath) else null
            
            if (keystoreFile != null && keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD").toString()
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS").toString()
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD").toString()
            } else {
                // Fallback to debug credentials so local builds succeed, but are still zip-aligned
                // and signed with v2/v3 signature schemes to avoid staging installation lag.
                val debugConfig = signingConfigs.getByName("debug")
                storeFile = debugConfig.storeFile
                storePassword = debugConfig.storePassword
                keyAlias = debugConfig.keyAlias
                keyPassword = debugConfig.keyPassword
            }
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
            // Only include actively maintained ABIs to prevent overflow issues
            include("arm64-v8a", "x86_64")
            isUniversalApk   = true    // single APK for sideloading (no architecture mismatch)
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
        // Limit list size to prevent potential overflow
        noCompress += listOf("db", "ttf", "otf")
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
