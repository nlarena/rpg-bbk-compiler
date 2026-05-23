import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.16.0"
    id("org.jetbrains.grammarkit") version "2023.3.0.3"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3")
    }
}

intellijPlatform {
    pluginConfiguration {
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
