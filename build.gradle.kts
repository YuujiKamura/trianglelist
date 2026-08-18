// This file is intentionally left blank for now.
// Subprojects will apply plugins directly.
// We might add common plugin applications here later if needed.
plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("com.android.kotlin.multiplatform.library") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.multiplatform") apply false
    // 1.61.0+ は Kotlin 2.3 メタデータでビルドされており本プロジェクトの Kotlin 2.0.0 では読めない
    id("io.github.takahirom.roborazzi") version "1.60.0" apply false
}

// ---------------------------------------------------------------------------
// Kotlin/Wasm の Yarn 解決オーバーライド (Dependabot alert 対応)
//
// kotlin-js-store/wasm/yarn.lock は Kotlin Gradle plugin の Yarn 統合が生成する。
// 中身は Kotlin 側ツールチェーン (webpack / karma / mocha / webpack-dev-server) の
// 推移依存で、直接編集しても kotlinWasmStoreYarnLock が上書き/検証で落とすため、
// KGP が用意している WasmYarnRootExtension.resolution() (= Yarn Classic の
// package.json "resolutions" フィールドを Gradle から書く仕組み) で pin する。
//
// pin を変えたら ./gradlew kotlinWasmUpgradeYarnLock を実行して lock を再生成すること。
//
// path は Yarn Classic の glob。"**/pkg" で top-level / nested どちらの出現も拾う。
// レンジは必ず major 上限を付けること ── 上限無し (">=2.0.10" 等) だと別 major に飛んで
// Node engine 不整合や CJS/ESM export 形状の変化で install/実行が壊れる
// (実際 http-proxy-middleware が 4.2.0 に飛んで node 22.0.0 と engine 不整合で落ちた)。
//
// なお kotlin-js-store/yarn.lock (JS ターゲット用) は Kotlin 2.2 移行で wasm 用 lock が
// kotlin-js-store/wasm/ に分離した際の残骸で、js() ターゲットも kotlinUpgradeYarnLock も
// 既に存在しない = 誰も生成/参照しない死んだファイルだったので削除済み。
// ---------------------------------------------------------------------------
plugins.withType(org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin::class.java).configureEach {
    val yarn = extensions.getByType(
        org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension::class.java
    )
    // 単一系統: 最小パッチ版以上に上げるだけ
    yarn.resolution("**/body-parser", ">=1.20.6 <2.0.0")
    yarn.resolution("**/diff", ">=8.0.3 <9.0.0")
    yarn.resolution("**/fast-uri", ">=3.1.5 <4.0.0")
    yarn.resolution("**/http-proxy-middleware", ">=2.0.10 <3.0.0")
    yarn.resolution("**/js-yaml", ">=4.3.1 <5.0.0")
    // serialize-javascript: 6.x に修正版が無く (advisory の範囲が >=5.0.0 <7.0.5)、
    // 唯一の patched version が 7.0.5 なので major を跨ぐ
    yarn.resolution("**/serialize-javascript", ">=7.0.5 <8.0.0")
    yarn.resolution("**/shell-quote", ">=1.9.0 <2.0.0")
    yarn.resolution("**/socket.io-parser", ">=4.2.7 <5.0.0")
    yarn.resolution("**/uuid", ">=11.1.1 <12.0.0")
    yarn.resolution("**/webpack", ">=5.104.1 <6.0.0")
    yarn.resolution("**/webpack-dev-server", ">=5.2.6 <6.0.0")
    yarn.resolution("**/ws", ">=8.21.0 <9.0.0")

    // brace-expansion は 1.x 系 (minimatch@3 経由) と 2.x 系 (minimatch@9 経由) が同居し
    // どちらの major も別々に脆弱だが、Yarn Classic の resolutions はマッチした全 path を
    // 同一バージョンに畳むため 2 系統を同時に別バージョンで pin することはできない
    // (include/exclude で ">=1.1.18 <2.0.0 || >=2.1.4" を渡すと両方 5.0.9 に畳まれた)。
    // さらに brace-expansion 5.x は `exports.expand` 形式で default export が無く、
    // minimatch@3 (`require(...)` を関数として呼ぶ) / minimatch@9 (default import) の
    // 両方が壊れる。よって両 advisory を同時に満たす最小の単一系統 = 2.1.4 系に寄せる。
    yarn.resolution("**/brace-expansion", ">=2.1.4 <3.0.0")
}

// 一括ビルド＆テストタスク
tasks.register("buildAndTest") {
    group = "verification"
    description = "ビルドとテストを一括実行"
    
    dependsOn(":app:assembleDevDebug")
    dependsOn(":app:testDevDebugUnitTest")
    dependsOn(":common:testAndroidHostTest")
    dependsOn(":desktop:test")
    
    doLast {
        println("✅ ビルドとテストが完了しました")
    }
} 