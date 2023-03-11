import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    // have to apply plugin here to have closures available in the subproject section
    kotlin("jvm") version Versions.kotlin
}

allprojects {
    group = Projects.baseGroupId
    version = Versions.project

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "idea")
    apply(plugin = "kotlin")

    dependencies {
        testImplementation(Dependencies.junitApi)
        testRuntimeOnly(Dependencies.junitEngine)
        implementation(kotlin(Dependencies.kotlinStdLibModule))
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
}
