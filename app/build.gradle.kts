plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.kymcoagility50"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.kymcoagility50"
        minSdk = 24
        targetSdk = 35
        // CI passes the run number so every GitHub Release carries an upgradable APK.
        versionCode = (System.getenv("APP_VERSION_CODE") ?: "2").toInt()
        versionName = System.getenv("APP_VERSION_NAME") ?: "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing: uses the keystore from CI secrets when provided (see README "Releases"),
    // otherwise falls back to the debug key so the release APK is still installable.
    val releaseKeystore = System.getenv("RELEASE_KEYSTORE_PATH")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 stays off: Room, kotlinx-serialization and Firebase reflection make a
            // shrunk build risky to ship untested, and the APK is dominated by the PDFs anyway.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (releaseKeystore != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    // The PDFs, OCR indexes and the Agility 50 knowledge base live in /shared so the website
    // ships exactly the same data; they're bundled here as flat assets.
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "../shared/docs", "../shared/content")
        }
    }

    // Owner manual PDF is large (scanned images); don't let AAPT try to compress it further / keep it accessible as a raw asset.
    androidResources {
        noCompress += "pdf"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    testOptions {
        unitTests {
            // Robolectric renders the Compose screens (with the bundled assets) on the JVM.
            isIncludeAndroidResources = true
            all {
                it.systemProperty("screenshots.dir", project.findProperty("screenshotsDir")?.toString() ?: "")
                it.maxHeapSize = "2g"
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    // Exported Room schemas (app/schemas) document every migration step.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // .await() on Task<T> (Firebase Auth / Firestore), used only from Sync-Mode-only code
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // ML Kit on-device text recognition (bundled model, offline after install)
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // DataStore for lightweight settings (also backs PrivacyManager's mode flag)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Sync Mode only (Firebase Auth + Firestore). Every call site is gated by
    // PrivacyManager.isPrivate() / NetworkGuard before any of this is touched, and
    // FirebaseApp.initializeApp() itself is never called while Private Mode is on.
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
