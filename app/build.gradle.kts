plugins {
    id("com.android.application")
}

android {
    namespace = "dev.novaroyale.tracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.novaroyale.tracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
