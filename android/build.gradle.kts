plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish") version "0.28.0"
}

android {
    namespace = "com.laudspeaker.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
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

dependencies {
    implementation("com.jakewharton.threetenabp:threetenabp:1.3.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("io.socket:socket.io-client:2.1.0")
    implementation("com.google.firebase:firebase-installations:17.2.0")
    implementation("com.google.firebase:firebase-messaging:23.4.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.google.code.gson:gson:2.10") // Use the latest version available
    implementation("io.sentry:sentry-android:7.8.0")
}