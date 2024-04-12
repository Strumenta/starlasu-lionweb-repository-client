import java.net.URI

plugins {
    java
    `jvm-test-suite`
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
    id("java-library")
    alias(libs.plugins.superPublish)
    alias(libs.plugins.buildConfig)
}

val rpgParserVersion = extra["rpgParserVersion"]
val javaModuleVersion = extra["javaModuleVersion"]
val eglParserVersion = extra["eglParserVersion"]
val kotestVersion = extra["kotestVersion"]
val kotlinVersion = extra["kotlinVersion"]

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
        url = uri("https://maven.pkg.github.com/Strumenta/kolasu-EGL")
        credentials {
            username = githubUser
            password = githubToken
        }
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        register<JvmTestSuite>("functionalTest") {
            dependencies {
                implementation(project())
                implementation(libs.kolasucore)
                implementation(libs.kolasulionweb)
                implementation(libs.kolasusemantics)
                implementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
                implementation("io.kotest:kotest-runner-junit5-jvm:5.8.0")
                implementation("com.strumenta.langmodules.kolasu-java-langmodule:ast:$javaModuleVersion")
                implementation("io.kotest.extensions:kotest-extensions-testcontainers:$kotestVersion")
                implementation("io.kotest:kotest-assertions-core:5.8.0")
                implementation("io.kotest:kotest-property:5.8.0")
                implementation("org.testcontainers:testcontainers:1.19.5")
                implementation("org.testcontainers:junit-jupiter:1.19.5")
                implementation("org.testcontainers:postgresql:1.19.5")
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

dependencies {
    implementation(libs.lwjava)
    implementation(libs.kolasucore)
    implementation(libs.kolasulionweb)
    implementation(libs.kolasusemantics)
    implementation(project(":lionweb-client"))

    testImplementation(kotlin("test-junit5"))
    testImplementation("com.strumenta.langmodules.kolasu-java-langmodule:ast:$javaModuleVersion")
    testImplementation("com.strumenta:rpg-parser:$rpgParserVersion")
    testImplementation(libs.eglast)
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

val lionwebRepositoryCommitID = extra["lionwebRepositoryCommitID"]

buildConfig {
    sourceSets.getByName("functionalTest") {
        packageName("com.strumenta.lwrepoclient.base")
        buildConfigField("String", "LIONWEB_REPOSITORY_COMMIT_ID", "\"${lionwebRepositoryCommitID}\"")
        useKotlinOutput()
    }
}
