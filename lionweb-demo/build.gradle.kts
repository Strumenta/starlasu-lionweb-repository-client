plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
    id("java-library")
}

val ktorVersion = extra["ktorVersion"]
val lionwebVersion = extra["lionwebVersion"]
val kolasuVersion = extra["kolasuVersion"]

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.lionweb.lionweb-java:lionweb-java-2023.1-core:$lionwebVersion")
    implementation(project(":lionweb-client"))
}
