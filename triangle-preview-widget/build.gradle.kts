plugins {
    kotlin("multiplatform") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
}

group = "com.jpaver.cadview"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
    }
    
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "cadview.js"
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
            }
        }
        
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        
        val jsMain by getting {
            dependencies {
                implementation(compose.web.core)
                implementation(compose.web.svg)
            }
        }
        
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, 
                         org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, 
                         org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "CADView"
            packageVersion = "1.0.0"
        }
    }
} 