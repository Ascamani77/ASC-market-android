import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.asc.markets"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.asc.markets"
        minSdk = 26
        targetSdk = 35
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

        buildConfigField("String", "REMOTE_CONFIG_URL", "\"${localProps.getProperty("REMOTE_CONFIG_URL") ?: project.findProperty("REMOTE_CONFIG_URL") ?: ""}\"")
        buildConfigField("String", "DEFAULT_BACKEND_URL", "\"${localProps.getProperty("BACKEND_URL") ?: project.findProperty("BACKEND_URL") ?: "http://20.109.163.27:8001"}\"")
        buildConfigField("boolean", "DEFAULT_FORCE_REMOTE", "${localProps.getProperty("DEFAULT_FORCE_REMOTE") ?: project.findProperty("DEFAULT_FORCE_REMOTE") ?: false}")
        buildConfigField("long", "DEFAULT_REMOTE_POLL_MS", "${localProps.getProperty("DEFAULT_REMOTE_POLL_MS") ?: project.findProperty("DEFAULT_REMOTE_POLL_MS") ?: 10000}L")
        buildConfigField("int", "TIINGO_THRESHOLD_LEVEL", "${localProps.getProperty("TIINGO_THRESHOLD_LEVEL") ?: project.findProperty("TIINGO_THRESHOLD_LEVEL") ?: 5}")
        buildConfigField("String", "DERIV_APP_ID", "\"${localProps.getProperty("DERIV_APP_ID") ?: project.findProperty("DERIV_APP_ID") ?: "1089"}\"")
        buildConfigField("boolean", "ENABLE_DEMO_SERVICES", "true")
    }

    signingConfigs {
        create("release") {
            val keystoreProps = Properties()
            val keystorePropsFile = project.rootProject.file("keystore.properties")
            if (keystorePropsFile.exists()) {
                keystoreProps.load(keystorePropsFile.inputStream())
            }
            storeFile = project.rootProject.file(keystoreProps.getProperty("storeFile", "keystore/asc-release.keystore"))
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "ENABLE_DEMO_SERVICES", "true")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "ENABLE_DEMO_SERVICES", "false")
            signingConfig = signingConfigs.getByName("release")
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

    lint {
        checkReleaseBuilds = false
        abortOnError = false
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
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
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

    // Ktor Client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Redis client
    implementation("redis.clients:jedis:5.1.0")

    // JSON
    implementation("org.json:json:20231013")
    
    // Markdown rendering for chat
    implementation("com.halilibo.compose-richtext:richtext-ui:0.17.0")
    implementation("com.halilibo.compose-richtext:richtext-commonmark:0.17.0")

    // Biometric authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)

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
