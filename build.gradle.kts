plugins {
    kotlin("jvm") version "2.3.10"
    application
}

group = "net.rafkos"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("net.rafkos.MainKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}