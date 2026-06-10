// プラグイン管理を一元化
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.11.1"
        id("com.android.library") version "8.11.1"
        id("org.jetbrains.kotlin.android") version "2.0.0"
        id("org.jetbrains.kotlin.jvm") version "2.0.0"
        id("org.jetbrains.kotlin.multiplatform") version "2.0.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
        id("org.jetbrains.compose") version "1.7.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        // Kotlin/Wasm ツールチェーン (Node.js / Binaryen / Yarn) は Kotlin plugin が
        // project レベルに ivy repo を登録して取得するが、PREFER_SETTINGS がそれを
        // 抑制するため settings 側に同じ repo を宣言する (Web 段階1 wasmJs ビルド用)。
        // content filter で対象 module 以外の解決には影響しない
        ivy("https://nodejs.org/dist") {
            name = "NodeJsDist"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy("https://github.com/WebAssembly/binaryen/releases/download") {
            name = "BinaryenDist"
            patternLayout { artifact("version_[revision]/[artifact]-version_[revision]-[classifier].[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.github.webassembly", "binaryen") }
        }
        ivy("https://github.com/yarnpkg/yarn/releases/download") {
            name = "YarnDist"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
    }
}

rootProject.name = "TriangleList"
include(":app")
include(":desktop")
include(":common")
// include(":core")  // 一時的にコメントアウト