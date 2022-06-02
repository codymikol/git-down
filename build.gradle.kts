import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.1"
    id("com.google.devtools.ksp") version "1.6.10-1.0.2"
}

val koin_version = "3.2.0"
val koin_ksp_version= "1.0.0-beta-2"
val ksp_version = "1.6.10-1.0.2"

group = "com.codymikol"
version = "1.0"

// Use KSP Generated sources
sourceSets.main {
    java.srcDirs("build/generated/ksp/main/kotlin")
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://download.eclipse.org/jgit/maven")
}

dependencies {
    api("io.insert-koin:koin-core:$koin_version")
    api("io.insert-koin:koin-annotations:$koin_ksp_version")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    ksp("io.insert-koin:koin-ksp-compiler:$koin_ksp_version")
    implementation("io.insert-koin:koin-core:$koin_version")
    implementation(compose.desktop.currentOs)
    implementation("net.harawata:appdirs:1.2.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.3")
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.0.202109080827-r")
    testImplementation("io.insert-koin:koin-test:$koin_version")
    testImplementation("io.kotest:kotest-runner-junit5:5.0.1")
    testImplementation("io.kotest:kotest-assertions-core:5.0.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "reboot-desktop"
            packageVersion = "1.0.0"
        }
    }
}