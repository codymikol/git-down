import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.google.ksp)
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://download.eclipse.org/jgit/maven")
}

dependencies {
    ksp(libs.koin.ksp)
    implementation(libs.koin.core)
    implementation(libs.koin.ksp)
    implementation(libs.jackson.module.kotlin)
    implementation(compose.desktop.currentOs)
    implementation(libs.appdirs)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    implementation(libs.jgit)
    testImplementation(libs.koin.test)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

// Use KSP Generated sources
sourceSets.main { java.srcDirs("build/generated/ksp/main/kotlin") }

tasks.withType<Test> { useJUnitPlatform() }

tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "17" }

group = "com.codymikol"
version = "1.0"

compose.desktop {
    application {
        mainClass = "com.codymikol.Main"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "gitdown"
            packageVersion = "1.0.0"
        }
    }
}