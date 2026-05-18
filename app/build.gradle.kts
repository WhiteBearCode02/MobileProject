plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.mobileproject"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.mobileproject"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // 코루틴 비동기 처리를 위한 핵심 라이브러리 (가산점 필수 요소)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 온디바이스 AI 구현을 위한 TensorFlow Lite Task Vision 라이브러리
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
}