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

    // JavaFX (C案 viewer)
    val javafxVersion = "21.0.2"
    val javafxPlatform = "win"
    implementation("org.openjfx:javafx-base:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-controls:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-swing:$javafxVersion:$javafxPlatform")

    // テスト依存関係
    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "MainKt"
        // Compose Desktop の Skia (Skiko) 描画を AWT Graphics に乗せて
        // BufferedImage / paintAll で screenshot 取れるようにする (CP capture コマンド用)。
        // 参考: JetBrains/compose-multiplatform-core PR #601 (Swing interop off-screen rendering)
        jvmArgs += listOf("-Dcompose.swing.render.on.graphics=true")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TriangleListDesktop"
            packageVersion = "1.0.0"
        }
    }
}

// 開発ループ高速化 (2026-08-27): viewer の runtime classpath をファイルに落とす。
// `./gradlew :desktop:run` は起動のたびに configuration + task graph で 20-30 秒かかるが、
// classpath さえ手元にあれば `java -cp @file MainKt` で数秒で立つ。
// コード変更時は :desktop:compileKotlin (インクリメンタル数秒) だけ回せばよい。
// 使い方は desktop/scripts/cad-dev.ps1 を参照。
tasks.register("dumpRuntimeClasspath") {
    group = "application"
    description = "viewer 起動用の classpath を build/viewer-classpath.txt に書き出す"
    val out = layout.buildDirectory.file("viewer-classpath.txt")
    val cp = sourceSets["main"].runtimeClasspath
    inputs.files(cp)
    outputs.file(out)
    doLast {
        // java の引数ファイル形式 (-cp @file)。Windows の \ は引数ファイル内で
        // エスケープ扱いになるため / に正規化する
        val sep = File.pathSeparator
        val joined = cp.joinToString(sep) { it.absolutePath.replace(File.separatorChar, '/') }
        out.get().asFile.writeText("-cp " + "\"" + joined + "\"" + System.lineSeparator())
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
// 依存は junit:junit:4.13.2 のため useJUnit() (Platform=JUnit 5 ではない)
tasks.test {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}