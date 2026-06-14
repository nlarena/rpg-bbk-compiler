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
    "rpg-frontend"
)
