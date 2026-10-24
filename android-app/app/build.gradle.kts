plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Every model's data (specs, schedule, search indexes, covers) ships in the APK; its PDFs never
// do: the app downloads one when the owner taps Download (data/models/ModelDocs.kt).
val scooterDataAssets = layout.buildDirectory.dir("generated/scooterData")
val syncScooterData by tasks.registering(Sync::class) {
    from(rootDir.resolve("../scooter-data")) { exclude("**/*.pdf", "README.md") }
    into(scooterDataAssets.map { it.dir("scooter-data") })
}

android {
    namespace = "io.github.achmalearning.scootercompanion"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.achmalearning.scootercompanion"
        minSdk = 24
        targetSdk = 35
        // CI passes the run number so every GitHub Release carries an upgradable APK.
        versionCode = (System.getenv("APP_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("APP_VERSION_NAME") ?: "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing uses the owner's private key, passed by CI from repository secrets (see
    // README "Releases"). Every release must be signed with that same key, or Android refuses
    // to install it as an update. Without the key, assembleRelease gives an unsigned APK.
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
            // R8 stays off: Room and kotlinx-serialization reflection make a
            // shrunk build risky to ship untested, and the APK is dominated by the PDFs anyway.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    // The Agility 50 knowledge base and the region packs live in /shared so the website ships
    // exactly the same data; they're bundled here as flat assets. /scooter-data (one folder per
    // scooter model) is bundled under "scooter-data/" without its PDFs, see syncScooterData.
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "../../shared/content", "../../shared/assets", scooterDataAssets)
        }
        // MigrationTestHelper reads the exported schemas as assets. Robolectric only sees the
        // merged app assets, so they ride in debug builds (a few KB), never in release.
        getByName("debug") {
            assets.srcDirs("$projectDir/schemas")
        }
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
    // Per-app language (French / English / Arabic) on every Android version
    implementation("androidx.appcompat:appcompat:1.7.0")
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

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // DataStore for lightweight settings and the first-launch flag
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

tasks.named("preBuild") { dependsOn(syncScooterData) }
