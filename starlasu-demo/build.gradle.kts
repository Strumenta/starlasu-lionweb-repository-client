plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
    id("java-library")
}

val ktorVersion = extra["ktorVersion"]
val lionwebVersion = extra["lionwebVersion"]
val kolasuVersion = extra["kolasuVersion"]

val githubUser =
    (
        project.findProperty("starlasu.github.user")
            ?: System.getenv("GITHUB_USER")
            ?: System.getenv("STRUMENTA_PACKAGES_USER")
    ) as? String
        ?: throw RuntimeException("GitHub user not specified")
val githubToken =
    (
        project.findProperty("starlasu.github.token")
            ?: System.getenv("GITHUB_TOKEN")
            ?: System.getenv("STRUMENTA_PACKAGES_TOKEN")
    ) as? String
        ?: throw RuntimeException("GitHub token not specified")

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = project.name
        url = uri("https://maven.pkg.github.com/Strumenta/rpg-parser")
        credentials {
            username = githubUser
            password = githubToken
        }
    }
    maven {
        name = project.name
        url = uri("https://maven.pkg.github.com/Strumenta/kolasu-EGL-langmodule")
        credentials {
            username = githubUser
            password = githubToken
        }
    }
}

val javaModuleVersion = extra["javaModuleVersion"]

dependencies {
    implementation("io.lionweb.lionweb-java:lionweb-java-2023.1-core:$lionwebVersion")
    implementation("com.strumenta.kolasu:kolasu-core:$kolasuVersion")
    implementation("com.strumenta.kolasu:kolasu-lionweb:$kolasuVersion")
    implementation(project(":starlasu-client"))
    implementation("com.strumenta.langmodules.kolasu-java-langmodule:ast:$javaModuleVersion")
}
