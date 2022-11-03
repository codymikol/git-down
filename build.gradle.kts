import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    id("org.jetbrains.compose") version "1.2.0"
    id("com.google.devtools.ksp") version "1.7.20-1.0.7"
}

val koinVersion = "3.2.2"
val koinKspVersion= "1.0.3"
val kspVersion = "1.6.10-1.0.2"

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
    api("io.insert-koin:koin-core:$koinVersion")
    api("io.insert-koin:koin-annotations:$koinKspVersion")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")
    ksp("io.insert-koin:koin-ksp-compiler:$koinKspVersion")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation(compose.desktop.currentOs)
    implementation("net.harawata:appdirs:1.2.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.4")
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0-rc1")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.3.0.202209071007-r")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.kotest:kotest-runner-junit5:5.5.2")
    testImplementation("io.kotest:kotest-assertions-core:5.5.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

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