
dependencies {
    implementation(Dependencies.slf4jApi)
    testImplementation(Dependencies.slf4jSimple)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(project(":util"))

    // todo move to module
    implementation("org.ktorm:ktorm-core:3.6.0")
    testImplementation("com.h2database:h2:2.1.212")
    testImplementation("org.postgresql:postgresql:42.5.4")

}
