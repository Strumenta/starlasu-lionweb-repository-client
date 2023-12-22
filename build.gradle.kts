plugins {
//    id("org.jetbrains.kotlin.jvm") version "1.9.22"
//    id("org.jetbrains.dokka") version "1.9.10"
//    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
//
//    id("java-library")
    id("net.researchgate.release") version "3.0.2"
}

tasks {
    wrapper {
        gradleVersion = "8.5"
        distributionType = Wrapper.DistributionType.ALL
    }
}

allprojects {

    group = "com.strumenta"

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

subprojects {

    tasks.withType<Test>().all {
        testLogging {
            showStandardStreams = true
            showExceptions = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}


