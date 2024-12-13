import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    id("com.vanniktech.maven.publish") version "0.30.0"
}

kotlin {
    // every class, method, property must declare there visibility
    explicitApi()

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
        publishLibraryVariants("release", "debug")
    }
    jvm()
    js {
        nodejs {
            testTask {
            }
        }
        binaries.executable()
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosX64()
    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()
    mingwX64()
    linuxX64()
    linuxArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.serialization)
            implementation(libs.kotlin.coroutines)
            implementation(libs.ktor.client.core)
            implementation(libs.okio)
        }
        jsMain.dependencies {
            api(libs.okio.nodefilesystem)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.okio.fakefilesystem)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlin.coroutines.test)
            implementation(libs.ktor.client.logging)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
        }
        androidNativeTest.dependencies {
            implementation(libs.slf4j.android)
        }
        jvmTest.dependencies {
            implementation(libs.slf4j.jvm)
        }
        jsTest.dependencies {
            api(libs.okio.nodefilesystem)
            implementation(npm("@js-joda/timezone", "2.3.0"))
        }
    }
}

android {
    namespace = "fr.frankois944.ktorfilecaching"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

mavenPublishing {
    // Define coordinates for the published artifact
    coordinates(
        groupId = "io.github.frankois944",
        artifactId = "ktorfilecaching",
        version = "0.3"
    )

    // Configure POM metadata for the published artifact
    pom {
        name.set("KMP Library for Ktor client file caching")
        description.set("This library can be used by a lot of targets for enabling the file caching of Ktor caching (https://ktor.io/docs/client-caching.html) ")
        inceptionYear.set("2024")
        url.set("https://github.com/frankois944/KtorKMPFileCaching")

        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://opensource.org/licenses/Apache-2.0")
            }
        }

        // Specify developer information
        developers {
            developer {
                id.set("frankois944")
                name.set("Francois Dabonot")
                email.set("dabonot.francois@gmail.com")
            }
        }

        // Specify SCM information
        scm {
            url.set("https://github.com/frankois944/KtorKMPFileCaching")
        }
    }

    // Configure publishing to Maven Central
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)

    // Enable GPG signing for all publications
    signAllPublications()
}