import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // [구글 공식 API Key 자동 은닉/주입 보안 플러그인 추가]
    // local.properties 내부의 GOOGLE_MAPS_API_KEY 변수를 추적하여 Manifest에 자동 바인딩합니다.
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1"
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

    // [인프라 설정]: XML 레이아웃 객체 참조 무결성 및 고속 결합을 위해 뷰바인딩 컴포넌트 활성화
    buildFeatures {
        viewBinding = true
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

    // 코루틴 비동기 처리 패키지
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // TensorFlow Lite 온디바이스 컴퓨터 비전 모델 추론 라이브러리
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")

    // [구글 지도 SDK 의존성 추가]: 구글 맵 렌더링 및 하드웨어 그래픽 가속을 위한 핵심 모바일 모듈
    implementation("com.google.android.gms:play-services-maps:18.2.0")
}