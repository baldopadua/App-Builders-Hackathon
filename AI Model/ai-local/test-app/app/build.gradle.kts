import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Test app for the ai-local core. It compiles the SAME sources and models as the real app:
//   kotlin/  -> ../../kotlin     assets/models/ -> ../../assets/models
// No copying, so what you test here is exactly what the Flutter app will bundle.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.secondlook.aitest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.secondlook.aitest"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")   // test only; sideload-able
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            java.srcDir("../../kotlin")
            assets.srcDir("../../assets")
        }
    }

    androidResources {
        noCompress += "onnx"   // keep models uncompressed so they load fast
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.26.0")
}
