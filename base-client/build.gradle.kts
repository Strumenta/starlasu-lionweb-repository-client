plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.10"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"

    id("java-library")
    id("com.vanniktech.maven.publish") version "0.26.0"
}

val ktorVersion = extra["ktorVersion"]
val lionwebVersion = extra["lionwebVersion"]
val kolasuVersion = extra["kolasuVersion"]

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.lionweb.lionweb-java:lionweb-java-2023.1-core:$lionwebVersion")
}

mavenPublishing {
    coordinates("com.strumenta.lwrepoclient", "lwrepoclient-base", version as String)

    pom {
        name.set("lwrepoclient-base")
        description.set("The Kotlin client for the lionweb-repository")
        inceptionYear.set("2023")
        url.set("https://github.com/Strumenta/starlasu-lionweb-repository-client")
//        licenses {
//            license {
//                name.set("The Apache License, Version 2.0")
//                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
//                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
//            }
//        }
//        developers {
//            developer {
//                id.set("username")
//                name.set("User Name")
//                url.set("https://github.com/username/")
//            }
//        }
//        scm {
//            url.set("https://github.com/username/mylibrary/")
//            connection.set("scm:git:git://github.com/username/mylibrary.git")
//            developerConnection.set("scm:git:ssh://git@github.com/username/mylibrary.git")
//        }
    }
}
