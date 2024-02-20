import java.net.URI

plugins {
    java
    `jvm-test-suite`
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.10"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"

    id("java-library")
    id("com.vanniktech.maven.publish") version "0.27.0"
}

val ktorVersion = extra["ktorVersion"]
val lionwebVersion = extra["lionwebVersion"]
val kolasuVersion = extra["kolasuVersion"]
val okhttpVersion = extra["okhttpVersion"]
val jvmVersion = extra["jvmVersion"] as String
val kotestVersion = extra["kotestVersion"]
val kotlinVersion = extra["kotlinVersion"]

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("io.lionweb.lionweb-java:lionweb-java-2023.1-core:$lionwebVersion")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        register<JvmTestSuite>("functionalTest") {
            dependencies {
                implementation(project())
                implementation("com.strumenta.kolasu:kolasu-core:$kolasuVersion")
                implementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
                implementation("io.kotest:kotest-runner-junit5-jvm:5.8.0")
                implementation("io.kotest.extensions:kotest-extensions-testcontainers:$kotestVersion")
                implementation("io.kotest:kotest-assertions-core:5.8.0")
                implementation("io.kotest:kotest-property:5.8.0")
                implementation("org.testcontainers:testcontainers:1.19.5")
                implementation("org.testcontainers:junit-jupiter:1.19.5")
                implementation("org.testcontainers:postgresql:1.19.5")
                implementation("io.lionweb.lionweb-java:lionweb-java-2023.1-core:$lionwebVersion")
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
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
    coordinates("com.strumenta.lwrepoclient", "lwrepoclient-base", version as String)

    pom {
        name.set("lwrepoclient-base")
        description.set("The Kotlin client for the lionweb-repository")
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}