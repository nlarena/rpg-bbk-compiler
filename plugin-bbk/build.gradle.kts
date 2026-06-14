import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.16.0"
    id("org.jetbrains.grammarkit") version "2023.3.0.3"
}

// Release version for the published plugin (overrides the root SNAPSHOT so the
// distribution artifact is named plugin-bbk-1.0.0.zip).
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
    // For the round-trip validation test: translate RPG with the frontend, then
    // confirm the generated BBK parses + type-checks with this plugin.
    testImplementation(project(":rpg-frontend"))
}

intellijPlatform {
    pluginConfiguration {
        // Release version for the Marketplace (overrides the root SNAPSHOT version,
        // which is fine for the other modules but not for a published artifact).
        version = "1.0.0"
        ideaVersion {
            sinceBuild = "253"
            untilBuild = provider { null }
        }
    }
}

// ----- JFlex lexer generation -----

val generateBbkLexer = tasks.register<GenerateLexerTask>("generateBbkLexer") {
    sourceFile.set(file("src/main/grammar/BBK.flex"))
    targetOutputDir.set(file("src/main/gen/com/larena/boxbreaker/plugin/bbk"))
    purgeOldFiles.set(true)
}

val generateBbkParser = tasks.register<GenerateParserTask>("generateBbkParser") {
    sourceFile.set(file("src/main/grammar/BBK.bnf"))
    targetRootOutputDir.set(file("src/main/gen"))
    pathToParser.set("com/larena/boxbreaker/plugin/bbk/parser/BbkParser.java")
    pathToPsiRoot.set("com/larena/boxbreaker/plugin/bbk/psi")
    purgeOldFiles.set(true)
}

sourceSets["main"].java.srcDir("src/main/gen")

tasks.named("compileJava") {
    dependsOn(generateBbkLexer, generateBbkParser)
}
