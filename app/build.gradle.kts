val cameraxVersion = "1.3.1" // Moved from dependencies block

plugins {
    // Corrected plugin syntax
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.scrollin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.scrollin"
        minSdk = 26 // Updated to API 26 as per our initial plan for better feature support
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Use 'isMinifyEnabled' in Kotlin DSL
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8 // Corrected syntax
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Add viewBinding for easier access to UI components
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Core Android & UI Libraries
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0") // Updated to the newer version
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // CameraX Dependencies (as per our project plan)
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ML Kit Dependencies - CORRECTED
    implementation("com.google.mlkit:pose-detection-accurate:18.0.0-beta3")
    implementation("com.google.mlkit:face-detection:16.1.6") // Corrected library

    // Testing Libraries
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
