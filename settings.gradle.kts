pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "BoxBreaker"

include(
    "plugin-bbk",
    "boxbreaker-ide",
    "rpg-frontend",
    "debugger",
    "bbk-debugger",
    "bbk-jvm-debug",
    "bbk-core",
    "bbk-runtime"
)
