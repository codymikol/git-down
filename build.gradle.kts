import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.shadow.jar)
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
    implementation(libs.shadow.jar)
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.components.resources)
    testImplementation(libs.koin.test)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}


compose.resources {
    publicResClass = true
}

// Use KSP Generated sources
sourceSets.main { java.srcDirs("build/generated/ksp/main/kotlin") }

configurations.all {
  resolutionStrategy {
    failOnNonReproducibleResolution()
  }
}

tasks.withType<Test>().configureEach { 
  useJUnitPlatform() 
  testLogging {
        events("passed", "skipped", "failed")
  }
}

tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "21" }

group = "com.codymikol"
version = "1.0"

tasks.jar {
  manifest {
    attributes["Main-Class"] = "com.codymikol.Main"
    attributes["Class-Path"] = configurations
        .runtimeClasspath
        .get()
        .joinToString(separator = " ") { file -> "libs/${file.name}" }
  }

  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
}

compose.desktop {
    application {
        mainClass = "com.codymikol.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "gitdown"
            packageVersion = "1.0.0"
        }
    }
}
