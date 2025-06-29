import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.ui)
    
    // テスト依存関係
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
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

// テストタスクの明示的な設定
tasks.test {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}