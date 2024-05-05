plugins {
    kotlin("jvm") version "1.9.23"
    application // to add support for building CLI application in Java.
    id("io.ktor.plugin") version "2.3.10" // Check if it's latest
}

group = "org.elcheapo"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("com.github.ajalt.clikt:clikt:4.4.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("org.elcheapo.MainKt") // The main class of the application
}

ktor {
    fatJar {
        archiveFileName.set("bafreader.jar")
    }
}