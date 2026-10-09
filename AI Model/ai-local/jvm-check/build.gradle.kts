// PC-only check: compiles the SAME Kotlin sources as the phone app (minus android/), runs the real ONNX
// models on the laptop against synthetic messages. Not shipped. Run: gradlew run
plugins {
    kotlin("jvm") version "2.1.20"
    application
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime:1.26.0")
    implementation("org.json:json:20240303")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
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

tasks.register<JavaExec>("gate3Eval") {
    group = "verification"
    description = "Evaluate the production NoteMatcher on held-out Gate 3 TSV cases"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.secondlook.check.Gate3EvalKt")
    args(
        providers.gradleProperty("gate3Model").orElse("../assets/models/embedding/model_quantized.onnx").get(),
        providers.gradleProperty("gate3Cases").orElse("../gate3/private_notes_draft.tsv").get(),
    )
}

tasks.register<JavaExec>("gate3Parity") {
    group = "verification"
    description = "Emit Kotlin ONNX embeddings for comparison with the Python evaluator"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.secondlook.check.Gate3ParityKt")
    args(
        providers.gradleProperty("gate3Model").orElse("../assets/models/embedding/model_quantized.onnx").get(),
        "../gate3/parity_texts.tsv",
    )
}
