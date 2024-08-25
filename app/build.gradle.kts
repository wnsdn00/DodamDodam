import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-android")
    id("kotlin-parcelize")
}

// Properties 파일을 로드하는 함수
fun loadProperties(file: File): Properties {
    val properties = Properties()
    file.inputStream().use { properties.load(it) }
    return properties
}

// local.properties 파일 로드
val localProperties = loadProperties(rootProject.file("local.properties"))

android {
    namespace = "com.explorit.dodamdodam"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.explorit.dodamdodam"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"${localProperties["kakao_native_app_key"]}\"")
        resValue("string", "kakao_oauth_host", localProperties["kakao_oauth_host"] as String)
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

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += "META-INF/NOTICE.md"
            excludes += "META-INF/LICENSE.md"
        }
    }
}

dependencies {
    // Kotlin 표준 라이브러리
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")

    // Firebase 라이브러리
    implementation(platform("com.google.firebase:firebase-bom:32.3.1"))
    implementation("com.google.firebase:firebase-analytics") // 파이어베이스 앱 분석
    implementation("com.google.firebase:firebase-auth-ktx") // 파이어베이스 인증
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Kakao 라이브러리
    implementation ("com.kakao.sdk:v2-all:2.20.3") // 전체 모듈 설치, 2.11.0 버전부터 지원
    implementation ("com.kakao.sdk:v2-user:2.20.3") // 카카오 로그인 API 모듈
    implementation ("com.kakao.sdk:v2-share:2.20.3") // 카카오톡 공유 API 모듈
    implementation ("com.kakao.sdk:v2-talk:2.20.3") // 카카오톡 채널, 카카오톡 소셜, 카카오톡 메시지 API 모듈
    implementation ("com.kakao.sdk:v2-friend:2.20.3") // 피커 API 모듈
    implementation ("com.kakao.sdk:v2-navi:2.20.3") // 카카오내비 API 모듈
    implementation ("com.kakao.sdk:v2-cert:2.20.3") // 카카오톡 인증 서비스 API 모듈

    // Google 라이브러리
    implementation ("com.google.android.gms:play-services-auth:20.7.0") // Google 로그인

    // Glide 라이브러리
    implementation("com.github.bumptech.glide:compiler:4.12.0") // 글라이드 이미지 로딩1
    implementation("com.github.bumptech.glide:glide:4.12.0") // 글라이드 이미지 로딩2

    //  라이브러리
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    implementation("com.android.volley:volley:1.2.1")

    // AndroidX 라이브러리
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation (libs.circleimageview)
    implementation(libs.volley)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}