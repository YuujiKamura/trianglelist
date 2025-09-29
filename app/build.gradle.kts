import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jpaver.myapplication"
        minSdk = 26
        targetSdk = 35
        versionCode = 1348
        versionName = "7.60"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    
    flavorDimensions.add("mode")

    productFlavors {
        create("dev") {
            dimension = "mode"
            applicationId = "com.jpaver.myapplication"
            targetSdk = 35
        }
        create("free") {
            dimension = "mode"
            applicationId = "com.jpaver.myapplication"
            targetSdk = 35
        }
        create("full") {
            dimension = "mode"
            applicationId = "com.paver.myapplication"
            targetSdk = 35
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    namespace = "com.jpaver.trianglelist"
    
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    
    lint {
        // baseline = file("lint-baseline.xml")  // 一時的に無効化して全警告を確認
        abortOnError = false  // 警告調査のため一時的にfalse
        warningsAsErrors = false
    }
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.3")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.gms:play-services-ads:24.5.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
    implementation("org.apache.poi:poi-ooxml:5.4.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.14.5")
    testImplementation("org.powermock:powermock-module-junit4:2.0.9")
    testImplementation("org.powermock:powermock-api-mockito:1.7.4")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.robolectric:robolectric:4.15.1")
    implementation(project(":common"))
    // implementation(project(":core"))  // 一時的にコメントアウト (lint警告確認用)
    testImplementation(project(":common"))
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

// カスタムタスク: コミット前チェック
tasks.register("preCommitChecks") {
    group = "verification"
    description = "コミット前の全チェックを実行"
    dependsOn("lintDevDebug", "testDevDebugUnitTest")
    
    doLast {
        println("✅ 全てのpre-commitチェックが完了しました！")
    }
}

// カスタムタスク: リリース準備
tasks.register("prepareRelease") {
    group = "release"
    description = "リリース準備: versionCode自動更新"
    notCompatibleWithConfigurationCache("Modifies version fields in build script at execution time")
    
    doLast {
        val buildFile = file("build.gradle.kts")
        val content = buildFile.readText()
        
        // 現在のバージョン取得
        val currentVersionCode = Regex("versionCode = (\\d+)").find(content)?.groupValues?.get(1)?.toInt()
        val currentVersionName = Regex("versionName = \"([^\"]+)\"").find(content)?.groupValues?.get(1)
        
        println("📱 現在のバージョン:")
        println("   versionCode: $currentVersionCode")
        println("   versionName: $currentVersionName")
        println("")
        
        if (currentVersionCode != null) {
            val newVersionCode = currentVersionCode + 1
            
            // versionCodeのみ自動更新
            val newContent = content.replace(
                Regex("versionCode = \\d+"),
                "versionCode = $newVersionCode"
            )
            buildFile.writeText(newContent)
            
            println("🔄 versionCode を $currentVersionCode → $newVersionCode に更新しました")
            println("⚠️  versionName は手動で確認・変更してください")
            println("")
            println("次の手順:")
            println("1. versionName を手動で更新")
            println("2. ./gradlew assembleRelease でビルド")
        } else {
            println("❌ versionCode の取得に失敗しました")
        }
    }
}
