# Ktor KMP File Caching
[![Tests](https://github.com/frankois944/KtorKMPFileCaching/actions/workflows/tests.yml/badge.svg)](https://github.com/frankois944/KtorKMPFileCaching/actions/workflows/tests.yml)
[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.frankois944/ktorfilecaching)](https://central.sonatype.com/artifact/io.github.frankois944/ktorfilecaching)


This project is a [Ktor client caching](https://ktor.io/docs/client-caching.html), which is (almost) literally a port of [FileStorage](https://api.ktor.io/ktor-client/ktor-client-core/io.ktor.client.plugins.cache.storage/-file-storage.html) but for KMP based on [OKIO](https://square.github.io/okio/multiplatform/) and [kotlinx serialization](https://github.com/Kotlin/kotlinx.serialization).


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

### Kotlin JS/Wasm for browser

```kotlin
// {project}/build.gradle.kts - Browser Js
jsMain.dependencies {
    implementation("io.github.frankois944:ktorfilecaching-jsbrowser:0.9.0")
    implementation(devNpm("copy-webpack-plugin", "9.1.0"))
}

// OR

// {project}/build.gradle.kts - Wasm JS
wasmJsMain.dependencies {
    implementation("io.github.frankois944:ktorfilecaching:0.9.0")
    implementation(devNpm("copy-webpack-plugin", "9.1.0"))
}
```

- Update or Create your webpack folder

```
// {project}/webpack.config.d/sqljs.js
config.resolve = {
    fallback: {
        fs: false,
        path: false,
        crypto: false,
    }
};

const CopyWebpackPlugin = require('copy-webpack-plugin');
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            '../../node_modules/sql.js/dist/sql-wasm.wasm'
        ]
    })
```

[More info on SqlDelight documentation](https://sqldelight.github.io/sqldelight/latest/js_sqlite/sqljs_worker/)

### Kotlin JS for NodeJS

```kotlin
implementation("io.github.frankois944:ktorfilecaching-jsnode:0.9.0")
```

### other platform

```kotlin
implementation("io.github.frankois944:ktorfilecaching:0.9.0")
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
