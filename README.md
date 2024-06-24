# Ktor KMP File Caching

This project is adding a file caching for [Ktor client caching](https://ktor.io/docs/client-caching.html).

Currently, there is a Memory caching, it's the default [HttpCache](https://ktor.io/docs/client-caching.html#memory_cache) behavior of the plugin and a [file caching only for Java Target](https://ktor.io/docs/client-caching.html#persistent_cache).

The main goal is to make available file caching to many KMP target as possible.

It's based on [OKIO dependency](https://square.github.io/okio/multiplatform/) and [kotlinx serialization](https://github.com/Kotlin/kotlinx.serialization).

The current supported targets are :

| Target                | Supported  |
|-----------------------|------------|
| jvm                   | ✅          |
| js Node               | ✅          |
| js browser            | ❌          |
| wasm                  | ❌          |
| iosX64                | ✅          |
| iosArm64              | ✅          |
| iosSimulatorArm64     | ✅          |
| macosX64              | ✅          |
| macosArm64            | ✅          |
| watchosArm32          | ✅          |
| watchosArm64          | ✅          |
| watchosSimulatorArm64 | ✅          |
| watchosX64            | ✅          |
| tvosSimulatorArm64    | ✅          |
| tvosX64               | ✅          |
| mingwX64              | ✅          |
| mingwX64              | ✅          |
| linuxX64              | ✅          |
| linuxArm64            | ✅          |
