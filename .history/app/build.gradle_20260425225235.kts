plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.asc.markets"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.asc.markets"
        minSdk = 26
        targetSdk = 34
        versionCode = 20260131
        versionName = "1.0.0-20260131"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "OPENAI_API_KEY", "\"${project.findProperty("OPENAI_API_KEY") ?: ""}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"AIzaSyCSgl9f2fN-E6P9AeZ-1Qb_y1vkfplMmHc\"")
        buildConfigField("String", "REMOTE_CONFIG_URL", "\"${project.findProperty("REMOTE_CONFIG_URL") ?: ""}\"")
        buildConfigField("boolean", "DEFAULT_FORCE_REMOTE", "${project.findProperty("DEFAULT_FORCE_REMOTE") ?: false}")
        buildConfigField("long", "DEFAULT_REMOTE_POLL_MS", "${project.findProperty("DEFAULT_REMOTE_POLL_MS") ?: 10000}L")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("main") {
            // Reuse chart code from external project while it evolves independently.
            java.srcDirs(
                "C:/Users/HP/AndroidStudioProjects/ChartPage/app/src/main/kotlin/com/trading/app/components",
                "C:/Users/HP/AndroidStudioProjects/ChartPage/app/src/main/kotlin/com/trading/app/models",
                "C:/Users/HP/AndroidStudioProjects/ChartPage/app/src/main/kotlin/com/trading/app/indicators",
                "C:/Users/HP/AndroidStudioProjects/ChartPage/app/src/main/kotlin/com/trading/app/utils",
                "C:/Users/HP/AndroidStudioProjects/ChartPage/app/src/main/kotlin/com/trading/app/data",
                "C:/Users/HP/AndroidStudioProjects/ChartPage/app/src/main/kotlin/com/trading/app/ui/chart"
            )
        }
    }
}

// Remove KAPT configuration as we moved to KSP
// kapt {
//    correctErrorTypes = true
// }

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("androidx.webkit:webkit:1.7.0")
    implementation("com.tradingview:lightweightcharts:4.0.0")
    implementation("javax.inject:javax.inject:1")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.itextpdf:itext-core:8.0.2")

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Redis client
    implementation("redis.clients:jedis:5.1.0")

    // Gemini SDK updated from 0.2.0 to 0.9.0 to support responseMimeType
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    // JSON
    implementation("org.json:json:20231013")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
