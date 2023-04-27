import org.jetbrains.kotlin.gradle.tasks.KotlinTest

dependencies {
    testImplementation("org.testcontainers:testcontainers:1.17.6")
    testImplementation("org.testcontainers:junit-jupiter:1.17.6")
    testImplementation(Dependencies.slf4jSimple)
    testImplementation(project(":lock"))
    testImplementation(project(":impl"))
    testImplementation(project(":util"))
    testImplementation("org.postgresql:postgresql:42.6.0")
    testImplementation("mysql:mysql-connector-java:8.0.32")
}

val test: Test by tasks
test.onlyIf { project.hasProperty("integration-test") }
