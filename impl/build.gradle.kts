import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm") version "1.8.10"
}

group = "io.github.pak3nuh.messaging"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.slf4j:slf4j-api:2.0.6")
    testImplementation("org.slf4j:slf4j-simple:2.0.6")
    implementation("com.google.code.gson:gson:2.10.1")

    // todo move to module
    implementation("org.ktorm:ktorm-core:3.6.0")
    testImplementation("com.h2database:h2:2.1.212")
    testImplementation("org.postgresql:postgresql:42.5.4")

}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

val compileJava: JavaCompile by tasks
compileJava.targetCompatibility = "1.8"

val compileTestJava: JavaCompile by tasks
compileTestJava.targetCompatibility = "1.8"

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}