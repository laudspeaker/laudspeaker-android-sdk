// Top-level build file where you can add configuration options common to all sub-projects/modules.

import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0-Beta5" apply false
    id("com.vanniktech.maven.publish") version "0.28.0"
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0-Beta5")
    }
}
