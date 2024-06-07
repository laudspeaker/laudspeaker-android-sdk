// Top-level build file where you can add configuration options common to all sub-projects/modules.

import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0-Beta5" apply false
    id("com.vanniktech.maven.publish") version "0.28.0"
}

mavenPublishing {
    // Define coordinates for the published artifact
    coordinates(
        groupId = "com.laudspeaker",
        artifactId = "laudspeaker-android",
        version = "1.0.0"
    )

    // Configure POM metadata for the published artifact
    pom {
        name.set("Laudspeaker Android SDK")
        description.set("Android SDK for the Laudspeaker Customer Engagement Platform")
        inceptionYear.set("2024")

        url.set("https://github.com/laudspeaker/laudspeaker-android-sdk")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        // Specify developers information
        developers {
            developer {
                id.set("mcharawi")
                name.set("Mahamad Charawi")
                email.set("mahamad@laudspeaker.com")
            }
        }

        // Specify SCM information
        scm {
            url.set("https://github.com/laudspeaker/laudspeaker-android-sdk")
        }

    }

    // Configure publishing to Maven Central
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // Enable GPG signing for all publications
    signAllPublications()
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0-Beta5")
    }
}
