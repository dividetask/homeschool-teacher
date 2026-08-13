// :shared — pure-Kotlin module. No Android, Compose, or Storage code lives
// here; only data classes, registries, and pure algorithms. This is the
// piece that will move into a Kotlin Multiplatform `commonMain` source set
// when an iOS target is added.

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
