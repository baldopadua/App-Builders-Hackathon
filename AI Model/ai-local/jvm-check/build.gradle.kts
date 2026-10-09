// PC-only check: compiles the SAME Kotlin sources as the phone app (minus android/), runs the real ONNX
// models on the laptop against synthetic messages. Not shipped. Run: gradlew run
plugins {
    kotlin("jvm") version "2.1.20"
    application
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime:1.26.0")
    implementation("org.json:json:20240303")   // same API as Android's org.json; used by the Gate 3 evaluation
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

// gradlew fallbackCheck: the keyboard's regex fallback (Android App RuleBasedAiAdapter, copied read-only at build time)
// vs the ai-local core on the same messages. See src/fallback/kotlin/FallbackCheck.kt and handoff/android-app/README.md.
val keyboardFallbackSrc = layout.buildDirectory.dir("fallback-src")
val copyKeyboardFallback = tasks.register<Copy>("copyKeyboardFallback") {
    from("../../../Android App/android/app/src/main/kotlin/com/secondlook/ai") { include("AiDetectionAdapter.kt", "RuleBasedAiAdapter.kt") }
    into(keyboardFallbackSrc)
}
val fallback: SourceSet = sourceSets.create("fallback") {
    kotlin.srcDir("src/fallback/kotlin")
    kotlin.srcDir(keyboardFallbackSrc)
    compileClasspath += sourceSets["main"].output + sourceSets["main"].compileClasspath
    runtimeClasspath += sourceSets["main"].output + sourceSets["main"].runtimeClasspath
}
tasks.named("compileFallbackKotlin") { dependsOn(copyKeyboardFallback) }
tasks.register<JavaExec>("fallbackCheck") {
    group = "verification"
    description = "Keyboard regex fallback vs the ai-local core on samples.json + real PH SMS"
    classpath = fallback.runtimeClasspath
    mainClass.set("com.secondlook.check.FallbackCheckKt")
    workingDir = projectDir
    jvmArgs("-Xmx1g")
}

// Gate 3 (private-note matching, Tagalog / Taglish): gradlew gate3 [-Pargs="model=<dir> name=<label>"]. See Gate3.kt.
tasks.register<JavaExec>("gate3") {
    group = "verification"
    description = "Gate 3 evaluation of an embedding model with the app's own Kotlin matcher"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.secondlook.check.Gate3Kt")
    workingDir = projectDir
    args((project.findProperty("args") as String? ?: "").split(' ').filter { it.isNotBlank() })
    jvmArgs("-Xmx1g")
}
