// =============================================================================
// ARMOR-WATCH - wear/build.gradle.kts
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
// The Wear OS app: the screen that draws what WatchSession says (Compose for Wear OS) and the service that carries the messages of the Data Layer to it. Built only when the build is
// given -ParmorWear (it needs the Android SDK), by the workflow wear-build.yml. The rules are in the Kotlin core of the root project.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "es.electrohobby3d.armor.watch"
    compileSdk = 37
    defaultConfig {
        applicationId = "es.electrohobby3d.armor.watch"
        minSdk = 30
        targetSdk = 36
        versionCode = 3
        versionName = "0.0.3"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    testOptions { unitTests.isReturnDefaultValues = true }
    // The release APK leaves the build UNSIGNED and aligned by the workflow apk-build.yml: there is no signingConfig here, on purpose. The signature of the release (apksigner, the key of the
    // maintainer, the same one as ARMOR-ANDROID-CONTROL) is made on the maintainer's machine and the key never reaches the repository or GitHub Actions. Not shrunk: the app is small and has
    // never run on a watch, so nothing here is renamed or removed that could not be seen.
    buildTypes { release { isMinifyEnabled = false } }
    // Same bytes on every build: no list of dependencies (a signing blob) is written into the APK.
    dependenciesInfo { includeInApk = false; includeInBundle = false }
}

// The version lives here and in the manifest every other tool reads; a build stops when they differ.
afterEvaluate {
    val manifestVersion = (groovy.json.JsonSlurper().parse(rootProject.file("armor.project.json")) as Map<*, *>)["version"]
    val appVersion = android.defaultConfig.versionName
    check(manifestVersion == appVersion) { "armor.project.json says $manifestVersion but versionName is $appVersion: change them together (tools/armor_project_tool.py bump)" }
}

dependencies {
    implementation(project(":"))   // WatchProtocol, AlertInbox and WatchSession
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.wear.compose:compose-foundation:1.5.0")
    implementation("androidx.wear.compose:compose-material3:1.5.0")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")   // the Data Layer: the messages of the phone and the confirmation back
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")   // the Android stubs of org.json do nothing in local tests
}
