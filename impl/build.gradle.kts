
dependencies {
    implementation(Dependencies.slf4jApi)
    testImplementation(Dependencies.slf4jSimple)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(project(":util"))
    implementation(project(":lock"))
    api(project(":api"))

    // todo move to module
    implementation("org.ktorm:ktorm-core:3.6.0")
    testImplementation(project(":test-util"))

}
