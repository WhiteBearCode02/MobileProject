import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    // [수정]: 특정 버전을 명시(version "1.9.20")하면 프로젝트 전체 설정과 충돌하므로,
    // 중앙 버전 카탈로그(libs.versions.toml)에 정의된 버전을 사용하도록 alias로 변경합니다.
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.mobileproject"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mobileproject"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // [오류 수정]: java.util.Properties와 java.io.FileInputStream를 위에서 import하여 사용
        val properties = Properties()
        val propertiesFile = project.rootProject.file("local.properties")

        if (propertiesFile.exists()) {
            val inputStream = FileInputStream(propertiesFile)
            properties.load(inputStream)
            inputStream.close()
        }

        // 카카오 네이티브 앱 키 설정
        val kakaoKey = properties.getProperty("kakao_native_app_key") ?: ""
        manifestPlaceholders["KAKAO_MAP_KEY"] = kakaoKey
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

    // 코루틴 비동기 처리
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    // 카카오 지도 v2 SDK
    implementation("com.kakao.vectormap:android:2.9.5")
}