import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":common"))
    // implementation(project(":core"))  // 一時的にコメントアウト
    implementation(compose.desktop.currentOs)
    implementation(compose.ui)
    
    // テスト依存関係
    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TriangleListDesktop"
            packageVersion = "1.0.0"
        }
    }
}

// テストモード用のカスタムタスク
tasks.register<JavaExec>("runTest") {
    group = "application"
    description = "テキストジオメトリテストウィンドウを起動"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("MainKt")
    args("--test")
}

// テストタスクの設定
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}