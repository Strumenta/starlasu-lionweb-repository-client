import java.net.URI

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.10"
    id("org.jlleitschuh.gradle.ktlint") version "12.0.3"

    id("java-library")
    id("com.vanniktech.maven.publish") version "0.27.0"
}

val ktorVersion = extra["ktorVersion"]
val lionwebVersion = extra["lionwebVersion"]
val kolasuVersion = extra["kolasuVersion"]
val rpgParserVersion = extra["rpgParserVersion"]
val javaModuleVersion = extra["javaModuleVersion"]
val eglParserVersion = extra["eglParserVersion"]

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

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.lionweb.lionweb-java:lionweb-java-2023.1-core:$lionwebVersion")
    implementation("com.strumenta.kolasu:kolasu-core:$kolasuVersion")
    implementation("com.strumenta.kolasu:kolasu-lionweb:$kolasuVersion")
    implementation(project(":lionweb-client"))

    testImplementation(kotlin("test-junit5"))
    testImplementation("com.strumenta.langmodules.kolasu-java-langmodule:ast:$javaModuleVersion")
    testImplementation("com.strumenta:rpg-parser:$rpgParserVersion")
    testImplementation("com.strumenta.langmodules.kolasu-EGL-langmodule:ast:$eglParserVersion")
    testImplementation("com.strumenta:rpg-parser-symbol-resolution:$rpgParserVersion")
    testImplementation("commons-io:commons-io:2.7")
    testImplementation("org.slf4j:slf4j-simple:1.7.30")
}

publishing {
    repositories {
        maven {
            url = URI("https://maven.pkg.github.com/Strumenta/starlasu-lionweb-repository-client")
            credentials {
                username = (project.findProperty("starlasu.github.user") ?: System.getenv("starlasu_github_user")) as String?
                password = (project.findProperty("starlasu.github.token") ?: System.getenv("starlasu_github_token")) as String?
            }
        }
    }
}

mavenPublishing {
    coordinates("com.strumenta.lwrepoclient", "lwrepoclient-starlasu", version as String)

    pom {
        name.set("lwrepoclient-starlasu")
        description.set("The Kotlin client for working with StarLasu ASTSs and the lionweb-repository")
        inceptionYear.set("2023")
        url.set("https://github.com/Strumenta/starlasu-lionweb-repository-client")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("ftomassetti")
                name.set("Federico Tomassetti")
                url.set("https://github.com/ftomassetti/")
            }
        }
        scm {
            url.set("https://github.com/Strumenta/starlasu-lionweb-repository-client/")
            connection.set("scm:git:git://github.com/Strumenta/starlasu-lionweb-repository-client.git")
            developerConnection.set("scm:git:ssh://git@github.com/Strumenta/starlasu-lionweb-repository-client.git")
        }
    }
}

val jvmVersion = extra["jvmVersion"] as String

java {
    sourceCompatibility = JavaVersion.toVersion(jvmVersion)
    targetCompatibility = JavaVersion.toVersion(jvmVersion)
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = jvmVersion
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmVersion.removePrefix("1.")))
    }
}

afterEvaluate {
    tasks {
        named("generateMetadataFileForMavenPublication") {
            dependsOn("kotlinSourcesJar")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
