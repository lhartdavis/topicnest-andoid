plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
}

val javacpp by configurations.creating

android {
    namespace = "com.techtower.meetingtranscriber"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.techtower.meetingtranscriber"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            // Bytedeco FFmpeg 8.0.1 publishes Android native binaries for 64-bit ABIs.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDir(layout.buildDirectory.dir("javacpp/lib"))
        }
    }
}

kapt {
    correctErrorTypes = true
}

tasks.register<Copy>("javacppExtract") {
    dependsOn(javacpp)
    from(provider { javacpp.map { zipTree(it) } })
    include("lib/**")
    into(layout.buildDirectory.dir("javacpp"))
}

tasks.named("preBuild") {
    dependsOn("javacppExtract")
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.bytedeco.ffmpeg)
    implementation(libs.bytedeco.javacv)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.material3)

    kapt(libs.androidx.room.compiler)
    javacpp(libs.bytedeco.ffmpeg.platform.gpl)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
