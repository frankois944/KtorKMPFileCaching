import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // every class, method, property must declare there visibility
    explicitApi()

    androidTarget {
        // https://youtrack.jetbrains.com/issue/KT-66448
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    jvm()
    js {
        nodejs()
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
        jsTest.dependencies {
            api(libs.okio.nodefilesystem)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.okio.fakefilesystem)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlin.coroutines.test)
            implementation(libs.ktor.client.logging)
        }
        androidNativeTest.dependencies {
            implementation(libs.slf4j.android)
        }
        jvmTest.dependencies {
            implementation(libs.slf4j.jvm)
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
