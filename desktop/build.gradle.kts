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

// DXF分析ツール
tasks.register<JavaExec>("analyzeDxf") {
    group = "application"
    description = "DXFファイルのサイズ分析"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("DxfAnalyzerMainKt")
}

// DXF横断歩道修正ツール
tasks.register<JavaExec>("fixCrosswalk") {
    group = "application"
    description = "横断歩道をNo.3+11起点に修正"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("DxfCrosswalkFixerKt")
}

// テストタスクの設定
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}