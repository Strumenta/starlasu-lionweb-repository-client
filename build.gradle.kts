plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.dokka") version "1.9.10"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"

    id("java-library")
    id("net.researchgate.release") version "3.0.2"
}

repositories {
    mavenCentral()
}

group = "com.strumenta"

tasks {
    wrapper {
        gradleVersion = "8.5"
        distributionType = Wrapper.DistributionType.ALL
    }
}

tasks.withType<Test>().all {
    testLogging {
        showStandardStreams = true
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}


//kotlin {
//
//explicitApiWarning()
//compilerOptions {
//    apiVersion.set(KotlinVersion.KOTLIN_1_9)
//    languageVersion.set(KotlinVersion.KOTLIN_1_9)
//
//    jvmTarget.set(JvmTarget.JVM_1_8)
//    freeCompilerArgs.add("-Xjvm-default=all")
//}
//    }
//
//val java = project.javaExtension
//java.sourceCompatibility = JavaVersion.VERSION_1_8
//java.targetCompatibility = JavaVersion.VERSION_1_8