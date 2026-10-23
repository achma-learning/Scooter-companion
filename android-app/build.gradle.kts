// Top-level build file
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
    // Only needed for Sync Mode (Firebase Auth + Firestore). Parses app/google-services.json;
    // a syntactically valid placeholder ships in this repo so the build succeeds out of the
    // box, but Sync Mode will not actually authenticate until you swap in your own file —
    // see README.md "Sync Mode / Firebase setup".
    id("com.google.gms.google-services") version "4.4.2" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
