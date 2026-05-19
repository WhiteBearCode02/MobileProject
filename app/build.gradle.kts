// [코드 청정화]: 사용하지 않는 임포트 레코드(java.util.Properties, java.io.FileInputStream)를 삭제하여 Unused 경고를 소거합니다.

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // [보안 플러그인 주입]: local.properties 내부의 GOOGLE_MAPS_API_KEY 변수를 매니페스트에 자동 주입
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1"
}

android {
    namespace = "com.example.mobileproject"

    // [무결성 유지]: 최신 Jetpack 라이브러리가 요구하는 API 레벨인 36을 만족시켜 컴파일러 바이너리 파싱 환경을 수호합니다.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mobileproject"
        minSdk = 26

        // [경고 해결]: targetSdk 업그레이드 어시스턴트의 가이드성 알림 억제를 위해 기존 런타임 규격인 34로 롤백 정합합니다.
        // 이 조치를 통해 컴파일 타임 툴체인은 36을 참조하면서도, 런타임 경고 유발 요소를 완전히 원천 차단합니다.
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

    // [인프라 설정]: 뷰 컴포넌트 참조 무결성 및 고속 바인딩을 위해 뷰바인딩 활성화
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

    // [구글 지도 SDK 의존성]: 구글 맵 렌더링을 위한 핵심 모바일 모듈
    implementation("com.google.android.gms:play-services-maps:18.2.0")
}