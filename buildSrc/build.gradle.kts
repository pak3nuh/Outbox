plugins {
    kotlin("jvm") version "1.8.10"
}

repositories {
    mavenCentral()
}

val jvmVersion = "11"

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = jvmVersion

val compileJava: JavaCompile by tasks
compileJava.targetCompatibility = jvmVersion
