// =============================================================================
// ARMOR-WATCH - settings.gradle.kts
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "armor-watch"
// The Wear OS app needs the Android SDK, so it is part of the build only when it is asked for (-ParmorWear): the Kotlin core and its tests need nothing but a JDK.
if (providers.gradleProperty("armorWear").isPresent) include(":wear")
