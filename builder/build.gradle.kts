dependencies {
    implementation(Dependencies.slf4jApi)
    testImplementation(Dependencies.slf4jSimple)
    implementation(project(":impl"))
    implementation(project(":lock"))
    api(project(":api"))

    testImplementation(project(":test-util"))
    testImplementation("io.mockk:mockk:1.13.5")
}