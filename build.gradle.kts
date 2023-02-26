plugins {
    kotlin("jvm") version "1.8.10"
}

group = "io.github.pak3nuh.messaging"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}
