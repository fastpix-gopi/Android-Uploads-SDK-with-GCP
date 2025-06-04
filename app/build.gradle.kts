import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProps = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}
val secretKey: String? = localProps.getProperty("SECRET_KEY")
val tokenId: String? = localProps.getProperty("TOKEN_ID")

android {
    namespace = "io.fastpix.uploadsdk"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.fastpix.uploadsdk"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SECRET_KEY", "\"$secretKey\"")
        buildConfigField("String", "TOKEN_ID", "\"$tokenId\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    dataBinding {
        enable = true
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":uploader"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    // OkHttps
    implementation(libs.logging.interceptor)
    implementation (libs.okhttp)

    implementation(libs.androidx.lifecycle.runtime.ktx)
}