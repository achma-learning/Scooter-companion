# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    *** INSTANCE;
}
-keep,includedescriptorclasses class io.github.achmalearning.scootercompanion.**$$serializer { *; }
-keepclassmembers class io.github.achmalearning.scootercompanion.** {
    *** Companion;
}
