// プラグイン管理を一元化
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.6.1"
        id("com.android.library") version "8.6.1"
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
    }
}

rootProject.name = "TriangleList"
include(":app")
include(":desktop")
include(":common")
include(":core")