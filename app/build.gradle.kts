plugins {
    // Use version catalog plugin aliases so Gradle supplies plugin versions
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    alias(libs.plugins.kotlin.compose)
    // Kotlinx serialization plugin for @Serializable and Json
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.asc.markets"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.asc.markets"
        minSdk = 26
        targetSdk = 34
        // Bumped to ensure new installs replace older APKs on device
        // Use a date-based versionCode to avoid clashes with previously installed builds
        versionCode = 20260131
        versionName = "1.0.0-20260131"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // OpenAI API Key (leave previous where it was as requested)
        buildConfigField("String", "OPENAI_API_KEY", "\"${project.findProperty("OPENAI_API_KEY") ?: ""}\"")
        // Gemini API Key for the Research Center / News page
        buildConfigField("String", "GEMINI_API_KEY", "\"AIzaSyCSgl9f2fN-E6P9AeZ-1Qb_y1vkfplMmHc\"")
        
        // Optional remote config endpoint for staged feature flags (empty by default)
        buildConfigField("String", "REMOTE_CONFIG_URL", "\"${project.findProperty("REMOTE_CONFIG_URL") ?: ""}\"")
        // Default behavior for force-remote override (false unless explicitly set)
        buildConfigField("boolean", "DEFAULT_FORCE_REMOTE", "${project.findProperty("DEFAULT_FORCE_REMOTE") ?: false}")
        // Default poll interval (ms) used if not overridden in prefs (10_000 ms)
        buildConfigField("long", "DEFAULT_REMOTE_POLL_MS", "${project.findProperty("DEFAULT_REMOTE_POLL_MS") ?: 10000}L")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        // Enable generation of BuildConfig fields
        buildConfig = true
    }
    composeOptions {
        // set an appropriate compiler extension version for your Compose BOM / Kotlin version
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

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

    // Kotlinx serialization JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Retrofit and OkHttp for HTTP client
    implementation("com.squareup.retrofit2:retrofit:2.10.0")
    implementation("com.squareup.retrofit2:converter-gson:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // WebKit for WebView
    implementation("androidx.webkit:webkit:1.7.0")

    // TradingView Lightweight Charts Android wrapper
    implementation("com.tradingview:lightweightcharts:3.8.0")

    // Coil for image loading in Compose
    implementation("io.coil-kt:coil-compose:2.5.0")

    // iText for PDF generation
    implementation("com.itextpdf:itext-core:8.0.2")

    // Room persistence
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}