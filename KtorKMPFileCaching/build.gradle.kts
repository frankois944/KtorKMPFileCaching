import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    id("com.vanniktech.maven.publish") version "0.30.0"
}


val kotlinJsTargetAttribute = Attribute.of("kotlinJsTarget", String::class.java)

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
    js("jsNode") {
        nodejs {
            testTask {
                useMocha {
                    timeout = "5000" // Adjust as needed
                }
            }
        }
        attributes.attribute(kotlinJsTargetAttribute, targetName)
    }
    js("jsBrowser") {
        browser {
            testTask {
                useKarma {
                    useFirefoxHeadless()
                }
            }
        }
        attributes.attribute(kotlinJsTargetAttribute, targetName)
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "ktorfilecaching"
        browser {
            testTask {
                useKarma {
                    useFirefoxHeadless()
                }
            }
        }
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
            api(libs.okio)
        }
        val jsNodeMain by getting {
            dependencies {
                api(libs.okio.nodefilesystem)
            }
        }
        val jsBrowserMain by getting {
            dependencies {
                api(libs.okio.fakefilesystem)
            }
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
            api(libs.okio.fakefilesystem)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.okio.fakefilesystem)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlin.coroutines.test)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.datetime)
        }
        androidNativeTest.dependencies {
        }
        jvmTest.dependencies {
        }
        val jsNodeTest by getting {
        }
        val jsBrowserTest by getting {
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
        version = "0.4.1"
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