# Ktor KMP File Caching
[![Tests](https://github.com/frankois944/KtorKMPFileCaching/actions/workflows/tests.yml/badge.svg)](https://github.com/frankois944/KtorKMPFileCaching/actions/workflows/tests.yml)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.frankois944/ktorfilecaching)](https://central.sonatype.com/artifact/io.github.frankois944/ktorfilecaching)


This project is a [Ktor client caching](https://ktor.io/docs/client-caching.html), which is (almost) literally a port of [FileStorage](https://api.ktor.io/ktor-client/ktor-client-core/io.ktor.client.plugins.cache.storage/-file-storage.html) but for KMP based on [OKIO](https://square.github.io/okio/multiplatform/) and [kotlinx serialization](https://github.com/Kotlin/kotlinx.serialization).

> [!IMPORTANT]  
> The plugin uses the LocalStorage for the **wasm** target, there is a size limitation (about 5mo in total) who need to be manually managed by yourself.

## Example

```kotlin
private val publicStorageCaching = KtorFileCaching()
HttpClient {
    install(HttpCache) {
        publicStorage(publicStorageCaching)
    }
}
```

## Installation

### Kotlin JS for browser

```kotlin
implementation("io.github.frankois944:ktorfilecaching-jsbrowser:0.6.0")
```

### Kotlin JS for NodeJS

```kotlin
implementation("io.github.frankois944:ktorfilecaching-jsnode:0.6.0")
```

### other platform

```kotlin
implementation("io.github.frankois944:ktorfilecaching:0.6.0")
```

## Platforms

The current supported targets are :

| Target                | Supported |
|-----------------------|-----------|
| jvm                   | ✅         |
| js Node               | ✅         |
| js browser            | ✅         |
| wasmJS                | ✅         |
| iosX64                | ✅         |
| iosArm64              | ✅         |
| iosSimulatorArm64     | ✅         |
| macosX64              | ✅         |
| macosArm64            | ✅         |
| watchosArm32          | ✅         |
| watchosArm64          | ✅         |
| watchosSimulatorArm64 | ✅         |
| watchosX64            | ✅         |
| tvosSimulatorArm64    | ✅         |
| tvosX64               | ✅         |
| mingwX64              | ✅         |
| mingwX64              | ✅         |
| linuxX64              | ✅         |
| linuxArm64            | ✅         |
