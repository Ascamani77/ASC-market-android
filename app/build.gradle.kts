import java.util.Properties

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

        val localProps = Properties()
        val localPropsFile = project.rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localProps.load(localPropsFile.inputStream())
        }
        val demoProps = Properties()
        val demoPropsFile = project.rootProject.file("env.demo")
        if (demoPropsFile.exists()) {
            demoProps.load(demoPropsFile.inputStream())
        }

        buildConfigField("String", "OPENAI_API_KEY", "\"${localProps.getProperty("OPENAI_API_KEY") ?: project.findProperty("OPENAI_API_KEY") ?: ""}\"")
        buildConfigField("String", "GROQ_API_KEY", "\"${localProps.getProperty("GROQ_API_KEY") ?: project.findProperty("GROQ_API_KEY") ?: ""}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProps.getProperty("GEMINI_API_KEY") ?: project.findProperty("GEMINI_API_KEY") ?: ""}\"")
        buildConfigField("String", "REMOTE_CONFIG_URL", "\"${localProps.getProperty("REMOTE_CONFIG_URL") ?: project.findProperty("REMOTE_CONFIG_URL") ?: ""}\"")
        buildConfigField("boolean", "DEFAULT_FORCE_REMOTE", "${localProps.getProperty("DEFAULT_FORCE_REMOTE") ?: project.findProperty("DEFAULT_FORCE_REMOTE") ?: false}")
        buildConfigField("long", "DEFAULT_REMOTE_POLL_MS", "${localProps.getProperty("DEFAULT_REMOTE_POLL_MS") ?: project.findProperty("DEFAULT_REMOTE_POLL_MS") ?: 10000}L")
        buildConfigField("String", "TIINGO_API_KEY", "\"${localProps.getProperty("TIINGO_API_KEY") ?: project.findProperty("TIINGO_API_KEY") ?: ""}\"")
        buildConfigField("int", "TIINGO_THRESHOLD_LEVEL", "${localProps.getProperty("TIINGO_THRESHOLD_LEVEL") ?: project.findProperty("TIINGO_THRESHOLD_LEVEL") ?: 5}")
        buildConfigField("String", "FRED_API_KEY", "\"${localProps.getProperty("FRED_API_KEY") ?: project.findProperty("FRED_API_KEY") ?: ""}\"")
        buildConfigField("String", "DERIV_APP_ID", "\"${localProps.getProperty("DERIV_APP_ID") ?: project.findProperty("DERIV_APP_ID") ?: "1089"}\"")
        buildConfigField("String", "DERIV_API_TOKEN", "\"${localProps.getProperty("DERIV_API_TOKEN") ?: project.findProperty("DERIV_API_TOKEN") ?: ""}\"")
        buildConfigField("String", "BINANCE_API_KEY", "\"${localProps.getProperty("BINANCE_API_KEY") ?: project.findProperty("BINANCE_API_KEY") ?: ""}\"")
        buildConfigField("String", "BINANCE_SECRET_KEY", "\"${localProps.getProperty("BINANCE_SECRET_KEY") ?: project.findProperty("BINANCE_SECRET_KEY") ?: ""}\"")
        buildConfigField("String", "BINANCE_DEMO_API_KEY", "\"${demoProps.getProperty("BINANCE_DEMO_API_KEY") ?: project.findProperty("BINANCE_DEMO_API_KEY") ?: ""}\"")
        buildConfigField("String", "BINANCE_DEMO_SECRET_KEY", "\"${demoProps.getProperty("BINANCE_DEMO_SECRET_KEY") ?: project.findProperty("BINANCE_DEMO_SECRET_KEY") ?: ""}\"")
        
        // Binance Trading
        
        // cTrader Pepperstone Configuration (Live)
        buildConfigField("String", "CTRADER_HOST_TYPE", "\"${localProps.getProperty("CTRADER_HOST_TYPE") ?: project.findProperty("CTRADER_HOST_TYPE") ?: "live"}\"")
        buildConfigField("String", "CTRADER_CLIENT_ID", "\"${localProps.getProperty("CTRADER_CLIENT_ID") ?: project.findProperty("CTRADER_CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "CTRADER_CLIENT_SECRET", "\"${localProps.getProperty("CTRADER_CLIENT_SECRET") ?: project.findProperty("CTRADER_CLIENT_SECRET") ?: ""}\"")
        buildConfigField("String", "CTRADER_ACCESS_TOKEN", "\"${localProps.getProperty("CTRADER_ACCESS_TOKEN") ?: project.findProperty("CTRADER_ACCESS_TOKEN") ?: ""}\"")
        buildConfigField("String", "CTRADER_REFRESH_TOKEN", "\"${localProps.getProperty("CTRADER_REFRESH_TOKEN") ?: project.findProperty("CTRADER_REFRESH_TOKEN") ?: ""}\"")
        buildConfigField("String", "CTRADER_ACCOUNT_ID", "\"${localProps.getProperty("CTRADER_ACCOUNT_ID") ?: project.findProperty("CTRADER_ACCOUNT_ID") ?: ""}\"")
        buildConfigField("String", "CTRADER_BRIDGE_HOST", "\"${localProps.getProperty("CTRADER_BRIDGE_HOST") ?: project.findProperty("CTRADER_BRIDGE_HOST") ?: "192.168.1.100"}\"")
        buildConfigField("int", "CTRADER_BRIDGE_PORT", "${localProps.getProperty("CTRADER_BRIDGE_PORT") ?: project.findProperty("CTRADER_BRIDGE_PORT") ?: 8082}")
        
        // cTrader Pepperstone Configuration (Demo)
        buildConfigField("String", "CTRADER_DEMO_HOST_TYPE", "\"${localProps.getProperty("CTRADER_DEMO_HOST_TYPE") ?: project.findProperty("CTRADER_DEMO_HOST_TYPE") ?: "demo"}\"")
        buildConfigField("String", "CTRADER_DEMO_CLIENT_ID", "\"${localProps.getProperty("CTRADER_DEMO_CLIENT_ID") ?: project.findProperty("CTRADER_DEMO_CLIENT_ID") ?: ""}\"")
        buildConfigField("String", "CTRADER_DEMO_CLIENT_SECRET", "\"${localProps.getProperty("CTRADER_DEMO_CLIENT_SECRET") ?: project.findProperty("CTRADER_DEMO_CLIENT_SECRET") ?: ""}\"")
        buildConfigField("String", "CTRADER_DEMO_ACCESS_TOKEN", "\"${localProps.getProperty("CTRADER_DEMO_ACCESS_TOKEN") ?: project.findProperty("CTRADER_DEMO_ACCESS_TOKEN") ?: ""}\"")
        buildConfigField("String", "CTRADER_DEMO_REFRESH_TOKEN", "\"${localProps.getProperty("CTRADER_DEMO_REFRESH_TOKEN") ?: project.findProperty("CTRADER_DEMO_REFRESH_TOKEN") ?: ""}\"")
        buildConfigField("String", "CTRADER_DEMO_ACCOUNT_ID", "\"${localProps.getProperty("CTRADER_DEMO_ACCOUNT_ID") ?: project.findProperty("CTRADER_DEMO_ACCOUNT_ID") ?: ""}\"")
        buildConfigField("String", "CTRADER_DEMO_BRIDGE_HOST", "\"${localProps.getProperty("CTRADER_DEMO_BRIDGE_HOST") ?: project.findProperty("CTRADER_DEMO_BRIDGE_HOST") ?: "192.168.1.100"}\"")
        buildConfigField("int", "CTRADER_DEMO_BRIDGE_PORT", "${localProps.getProperty("CTRADER_DEMO_BRIDGE_PORT") ?: project.findProperty("CTRADER_DEMO_BRIDGE_PORT") ?: 8083}")
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-nowarn")
    }
}
