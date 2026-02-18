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
    implementation("org.apache.poi:poi-ooxml:5.4.1")

    // LWJGL (OpenGL viewer - B案)
    val lwjglVersion = "3.3.4"
    val lwjglNatives = "natives-windows"
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-stb:$lwjglVersion")
    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$lwjglNatives")

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

// DXF区画線抽出ツール
tasks.register<JavaExec>("extractMarkings") {
    group = "application"
    description = "DXFから区画線を抽出"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("DxfMarkingSyncToolKt")
}

// CSV → DXF 変換ツール
tasks.register<JavaExec>("csvToDxf") {
    group = "application"
    description = "三角形CSVからDXFファイルを生成"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("CsvToDxfMainKt")
}

// テストタスクの設定
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}