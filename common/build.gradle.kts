import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

// compose 依存は android/desktop の source set にしか無い (wasm から skiko を外すため)。
// compose compiler を wasmJs にも適用すると runtime 不在でエラーになるので JVM 系に限定する
composeCompiler {
    targetKotlinPlatforms.set(
        setOf(
            org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm,
            org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm
        )
    )
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    jvm("desktop")

    // Web 段階1 (insight #61): wasmJs ターゲット。境界は @JsExport の文字列 JSON 1 本
    // (WebFacade.kt)。binaries.executable() で common.mjs + .wasm を生成し、
    // web/ の Vite シェルが static asset として取り込む (kotlin-wasm-browser-template の流儀)。
    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl::class)
    wasmJs {
        // Kotlin 2.2.x で既定の出力名が "<project>" に変わった (旧: "<project>-wasm-js")。
        // web/ の import・fetch パス (sync-wasm.mjs / main.ts / uninstantiated の wasm 相対参照)
        // は旧名で固定済みなので、出力名を明示して互換を維持する
        outputModuleName.set("TriangleList-common-wasm-js")
        browser()
        binaries.executable()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        // compose 依存は JVM 側 (android/desktop) のみに置く。commonMain に置くと
        // wasmJs 実行物に skiko / @js-joda/core がリンクされ、Web 段階1 の
        // 「成果物を static asset として dynamic import」が bare import で壊れるため。
        // commonMain で compose を使っていたのは TextAlignUtils.kt (Offset) のみで、
        // 利用者が desktop だけだったので desktopMain へ移設済み
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.material3)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.material3)
            }
        }
    }
}

android {
    namespace = "com.jpaver.trianglelist.common"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }
}

// IDE Gradle panel から 1 クリックで web dev に wasm 反映するための束ね task。
// chain: (1) wasmJsBrowserDistribution で wasm build → (2) web/wasm に sync → (3) vite reload trigger。
val syncWebWasmExec = tasks.register<Exec>("syncWebWasmExec") {
    workingDir = layout.projectDirectory.dir("../web").asFile
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    commandLine = if (isWindows) listOf("cmd", "/c", "npm", "run", "sync-wasm")
                  else listOf("npm", "run", "sync-wasm")
    dependsOn("wasmJsBrowserDistribution")
}

abstract class TouchFileTask : DefaultTask() {
    @get:Internal
    abstract val targetFile: RegularFileProperty

    @TaskAction
    fun touch() {
        targetFile.get().asFile.setLastModified(System.currentTimeMillis())
    }
}

tasks.register<TouchFileTask>("syncWebDev") {
    group = "web"
    description = "wasm build + web/wasm sync + vite reload trigger (IDE Gradle panel から 1 クリック反映)"
    dependsOn(syncWebWasmExec)
    targetFile.set(layout.projectDirectory.file("../web/src/main.ts"))
}
