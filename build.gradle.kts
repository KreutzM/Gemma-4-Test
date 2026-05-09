plugins {
    id("com.android.application") version "8.8.2" apply false
    // Kotlin 2.2.0 is aligned with Google AI Edge Gallery and is required by
    // litertlm-android:0.11.0 Kotlin metadata.
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
}
