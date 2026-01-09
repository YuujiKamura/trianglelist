// This file is intentionally left blank for now.
// Subprojects will apply plugins directly.
// We might add common plugin applications here later if needed.
plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.multiplatform") apply false
}

// 一括ビルド＆テストタスク
tasks.register("buildAndTest") {
    group = "verification"
    description = "ビルドとテストを一括実行"
    
    dependsOn(":app:assembleDevDebug")
    dependsOn(":app:testDevDebugUnitTest")
    dependsOn(":common:testDebugUnitTest")
    dependsOn(":desktop:test")
    
    doLast {
        println("✅ ビルドとテストが完了しました")
    }
} 