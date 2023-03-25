
dependencies {
    implementation(Dependencies.slf4jApi)
    implementation(project(":util"))
    testImplementation(project(":test-util"))
    testImplementation(Dependencies.mokk)
    testImplementation(Dependencies.slf4jSimple)

}
