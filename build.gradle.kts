// =============================================================================
// ARMOR-WATCH - build.gradle.kts
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
// The Kotlin core of the watch client: the messages between the phone and the watch and the inbox of alerts. It has no Android in it, so its
// tests run on any computer with a JDK. The Wear OS screen and the Data Layer service that use it are not part of this build yet.
plugins {
    kotlin("jvm") version "2.4.21"
}

group = "armor.watch"
version = "0.0.1"

dependencies {
    implementation("org.json:json:20250517")
    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // the tests read docs/watch_alert_contract.json from the project folder
    workingDir = projectDir
}
