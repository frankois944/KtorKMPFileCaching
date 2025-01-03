# Ktor KMP File Caching

This project add a file caching for [Ktor client caching](https://ktor.io/docs/client-caching.html).

Currently, there is a Memory caching, it's the
default [HttpCache](https://ktor.io/docs/client-caching.html#memory_cache) behavior of the plugin and
a [file caching only for Java Target](https://ktor.io/docs/client-caching.html#persistent_cache).

The main goal is to make available file caching to many KMP target as possible.

It's based on [OKIO dependency](https://square.github.io/okio/multiplatform/)
and [kotlinx serialization](https://github.com/Kotlin/kotlinx.serialization).

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
implementation("io.github.frankois944:ktorfilecaching-jsbrowser:0.4.2")
```

### Kotlin JS for NodeJS

```kotlin
implementation("io.github.frankois944:ktorfilecaching-jsnode:0.4.2")
```

### other platform

```kotlin
implementation("io.github.frankois944:ktorfilecaching:0.4.2")
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
