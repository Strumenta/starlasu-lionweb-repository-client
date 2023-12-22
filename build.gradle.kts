plugins {
    id("net.researchgate.release") version "3.0.2"
    id("org.jetbrains.dokka") version "1.9.10" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
    id("com.vanniktech.maven.publish") version "0.26.0" apply false
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


