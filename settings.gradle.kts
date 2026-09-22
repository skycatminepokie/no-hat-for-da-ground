pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    // Check the latest version on https://stonecutter.kikugie.dev/blog/changes/0.9
    id("dev.kikugie.stonecutter") version "0.9.8"

    // Used for cross-compat for 26.1+ and older versions (https://codeberg.org/KikuGie/loom-back-compat)
    id("dev.kikugie.loom-back-compat") version "0.4.2"

    // Sometimes it is needed to make Gradle run at all, so it doesn't hurt to have
    // (https://github.com/gradle/foojay-toolchains)
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // See https://stonecutter.kikugie.dev/wiki/start/#choosing-minecraft-versions
        versions(
            "26.3",
            "26.2",
            "26.1",
            "1.21.9",
            "1.21.5",
            "1.21.2",
            "1.21",
            "1.20.5",
            "1.20.3",
            "1.20.2",
            "1.20",
            "1.19.4",
            "1.19.3",
            "1.19.2",
            "1.19",
        )
        vcsVersion = "26.3"
    }
}

rootProject.name = "No Hat For Da Ground"
