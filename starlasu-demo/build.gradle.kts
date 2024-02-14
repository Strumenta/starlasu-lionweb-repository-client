plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.10"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"

    id("java-library")
}

val ktorVersion = extra["ktorVersion"]
val lionwebVersion = extra["lionwebVersion"]
val kolasuVersion = extra["kolasuVersion"]
val rpgParserVersion = extra["rpgParserVersion"]
val javaModuleVersion = extra["javaModuleVersion"]
val eglParserVersion = extra["eglParserVersion"]

val githubUser = (
    project.findProperty("starlasu.github.user")
        ?: System.getenv("GITHUB_USER")
        ?: System.getenv("STRUMENTA_PACKAGES_USER")
    ) as? String
    ?: throw RuntimeException("GitHub user not specified")
val githubToken = (
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

dependencies {
    implementation("io.lionweb.lionweb-java:lionweb-java-2023.1-core:$lionwebVersion")
    implementation("com.strumenta.kolasu:kolasu-core:$kolasuVersion")
    implementation("com.strumenta.kolasu:kolasu-lionweb:$kolasuVersion")
    implementation(project(":starlasu-client"))
    implementation("com.strumenta.langmodules.kolasu-java-langmodule:ast:$javaModuleVersion")
    testImplementation(kotlin("test-junit5"))
    testImplementation("com.strumenta:rpg-parser:$rpgParserVersion")
    testImplementation("com.strumenta.langmodules.kolasu-EGL-langmodule:ast:$eglParserVersion")
    testImplementation("com.strumenta:rpg-parser-symbol-resolution:$rpgParserVersion")
    testImplementation("commons-io:commons-io:2.7")
    testImplementation("org.slf4j:slf4j-simple:1.7.30")
}

tasks.test {
    useJUnitPlatform()
}
