plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "br.com.autochamada"
    compileSdk = 35
    defaultConfig {
        applicationId = "br.com.autochamada"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
kotlin { jvmToolchain(17) }
