import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Test app for the ai-local core. It compiles the SAME sources and models as the real app:
//   kotlin/com/secondlook/ai/ -> the AI core      assets/models/ -> ../../assets/models
//   handoff/android-app/      -> OnnxAiAdapter + SecretManagerNotesStore (what the keyboard copies in)
//   the keyboard's own DetectionEngine, SecretManager, adapters and Warning, read from Android App at build time
// ai-local's own DetectionEngine.kt / SecretManager.kt are not compiled here: the keyboard app keeps Klarenz's.
// What you test here is exactly what the keyboard will run.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Keyboard mirror: copies (never edits) the keyboard's detection files from Android App into build/keyboard-src,
// so the test app always runs Klarenz's CURRENT DetectionEngine + SecretManager with the handoff adapter.
val keyboardApp = file("../../../../Android App/android/app/src/main/kotlin/com/secondlook")
val keyboardSrc = layout.buildDirectory.dir("keyboard-src").get().asFile
val copyKeyboardSources = tasks.register("copyKeyboardSources") {
    inputs.files(fileTree(keyboardApp) { include("DetectionEngine.kt", "SecretManager.kt", "WarningBanner.kt", "ai/AiDetectionAdapter.kt", "ai/RuleBasedAiAdapter.kt") })
    outputs.dir(keyboardSrc)
    doLast {
        keyboardSrc.deleteRecursively()
        for (f in listOf("DetectionEngine.kt", "SecretManager.kt", "ai/AiDetectionAdapter.kt", "ai/RuleBasedAiAdapter.kt")) {
            File(keyboardApp, f).copyTo(File(keyboardSrc, f), overwrite = true)
        }
        // Warning + WarningType sit in WarningBanner.kt next to the keyboard's views: keep only those two declarations
        val banner = File(keyboardApp, "WarningBanner.kt").readText()
        val warningType = Regex("""enum class WarningType \{[^}]*\}""").find(banner)?.value ?: error("WarningType not found in WarningBanner.kt")
        val warning = Regex("""data class Warning\([^)]*\)""").find(banner)?.value ?: error("Warning not found in WarningBanner.kt")
        File(keyboardSrc, "Warning.kt").writeText("package com.secondlook\n\n// Copied from Android App WarningBanner.kt by the test app build.\n$warningType\n\n$warning\n")
    }
}
tasks.named("preBuild") { dependsOn(copyKeyboardSources) }

// Gate 3: gradlew assembleRelease -Pgate3Candidate=C:/dev/hackathon/gate3-work/models/gate3-minilm-tl
// copies that model into build/gate3-assets/models/embedding-candidate so the speed test can time it on the phone.
val gate3Candidate: String? = providers.gradleProperty("gate3Candidate").orNull
val gate3Assets = layout.buildDirectory.dir("gate3-assets").get().asFile
val gate3CandidateAssets = tasks.register<Copy>("gate3CandidateAssets") {
    enabled = gate3Candidate != null
    if (gate3Candidate != null) from(gate3Candidate) { include("model_quantized.onnx", "vocab.txt", "config.json") }
    into(File(gate3Assets, "models/embedding-candidate"))
}
tasks.named("preBuild") { dependsOn(gate3CandidateAssets) }

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
            java.srcDir("../../kotlin/com/secondlook/ai")
            java.srcDir("../../handoff/android-app")
            java.srcDir(keyboardSrc)
            assets.srcDir("../../assets")
            assets.srcDir("../../test-data/gate3")   // invented private-note set for the Gate 3 speed test (test app only)
            // -Pgate3Candidate=<model folder>: also bundle a candidate embedding model as models/embedding-candidate
            if (gate3Candidate != null) assets.srcDir(gate3Assets)
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
