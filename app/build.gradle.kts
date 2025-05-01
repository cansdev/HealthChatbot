import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.healthchatbotapp"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.healthchatbotapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // ──────────────────────────────────────────────────────────────
        // Load local.properties manually
        val propsFile = rootProject.file("local.properties")
        if (!propsFile.exists()) {
            throw GradleException("local.properties not found in project root")
        }
        val props = Properties().apply {
            load(FileInputStream(propsFile))
        }
        val apiKey = props.getProperty("apiKey")
            ?: throw GradleException("Please define 'apiKey' in local.properties")
        // Expose it as BuildConfig:
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
        // ──────────────────────────────────────────────────────────────

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.material)
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Gemini HTTP & JSON
    implementation(libs.okhttp)
    implementation(libs.gson)
    testImplementation(libs.junit.junit)
}
