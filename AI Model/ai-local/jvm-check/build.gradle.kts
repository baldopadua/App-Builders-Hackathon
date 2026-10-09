// PC-only check: compiles the SAME Kotlin sources as the phone app (minus android/), runs the real ONNX
// models on the laptop against synthetic messages. Not shipped. Run: gradlew run
plugins {
    kotlin("jvm") version "2.1.20"
    application
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime:1.26.0")
}

sourceSets {
    main {
        kotlin {
            srcDir("../kotlin")
            exclude("**/android/**", "**/bridge/**", "**/DetectionEngine.kt", "**/SecretManager.kt")   // need android.*
        }
    }
}

application {
    mainClass.set("com.secondlook.check.MainKt")
}
