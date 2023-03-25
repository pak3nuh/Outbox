plugins {
    kotlin("jvm") version "1.8.10"
}

repositories {
    mavenCentral()
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
}
