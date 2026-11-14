// =============================================================================
// ARMOR-WATCH - build.gradle.kts
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
// The Kotlin core of the watch client: the messages between the phone and the watch and the inbox of alerts. It has no Android in it, so its
// tests run on any computer with a JDK. The Wear OS app that uses it (wear/) is part of the build only with -ParmorWear, because it needs the Android SDK.
plugins {
    kotlin("jvm") version "2.4.21"
    // only put on the class path here; the Wear OS app (wear/) applies them when -ParmorWear is given
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}

group = "armor.watch"
version = "0.0.1"

dependencies {
    // Android has org.json built in, so the Wear OS app must not bring a second copy: the core only needs it to compile, and its tests bring the real library.
    compileOnly("org.json:json:20250517")
    testImplementation("org.json:json:20250517")
    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    // the tests read docs/watch_alert_contract.json from the project folder
    workingDir = projectDir
}
