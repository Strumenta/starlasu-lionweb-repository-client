import net.researchgate.release.ReleaseExtension

plugins {
    id("net.researchgate.release") version "3.0.2"
    id("org.jetbrains.dokka") version "1.9.10" apply(false)
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply(false)
    id("com.vanniktech.maven.publish") version "0.27.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

tasks {
    wrapper {
        gradleVersion = "8.5"
        distributionType = Wrapper.DistributionType.ALL
    }
}

allprojects {

    group = "com.strumenta.lionwebrepoclient"
    project.version = version

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

val isReleaseVersion = !(project.version as String).endsWith("SNAPSHOT")

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    tasks.withType<Test>().all {
        testLogging {
            showStandardStreams = true
            showExceptions = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    ktlint {
        filter {
            exclude { element ->
                element
                    .file
                    .absolutePath
                    .split(File.separator)
                    .contains("build")
            }
        }
    }
}

configure<ReleaseExtension> {
    buildTasks.set(
        listOf(
            ":lionweb-client:publish",
            ":starlasu-client:publish",
        ),
    )
    git {
        requireBranch.set("")
        pushToRemote.set("origin")
    }
}
