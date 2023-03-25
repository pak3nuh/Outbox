import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    // have to apply plugin here to have closures available in the subproject section
    // todo migrate to coding convention plugins
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

    val jvmVersion = Versions.jvm

    val compileJava: JavaCompile by tasks
    compileJava.targetCompatibility = jvmVersion

    val compileTestJava: JavaCompile by tasks
    compileTestJava.targetCompatibility = jvmVersion

    val compileKotlin: KotlinCompile by tasks
    compileKotlin.kotlinOptions {
        jvmTarget = jvmVersion
    }

    val compileTestKotlin: KotlinCompile by tasks
    compileTestKotlin.kotlinOptions {
        jvmTarget = jvmVersion
    }
}
