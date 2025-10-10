plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.evcharging.mobile" // Fixed namespace - removed duplicate
    compileSdk = 34

    defaultConfig {
        applicationId = "com.evcharging.mobile" // Fixed applicationId - removed duplicate
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema location
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }

    // BuildConfig fields
    buildConfigField("String", "API_BASE_URL", "\"https://evwebserverapi.onrender.com/\"")
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            buildConfigField("String", "API_BASE_URL", "\"https://evwebserverapi.onrender.com/\"")
        }
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"https://evwebserverapi.onrender.com/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

dependencies {
    // Android Core
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.swiperefreshlayout)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.runtime.ktx)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    annotationProcessor(libs.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    
    // Explicit OkHttp dependencies (fallback)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // QR Code
    implementation(libs.zxing.android.embedded)
    implementation(libs.zxing.core)

    // Location & Maps
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)

    // Image Loading
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // UI Components
    implementation(libs.sweetalert)
    implementation(libs.lottie)
    implementation(libs.shimmer)

    // Testing
    testImplementation(libs.junit)
    // FIXED: Remove or comment out the problematic line
    // testImplementation(libs.room.testing) // This causes the error
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}