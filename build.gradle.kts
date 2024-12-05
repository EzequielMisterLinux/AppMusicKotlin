plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("com.google.gms.google-services") version "4.3.15" apply false
}

// Al final de build.gradle.kts (nivel de aplicación)
apply(plugin = "com.google.gms.google-services")
