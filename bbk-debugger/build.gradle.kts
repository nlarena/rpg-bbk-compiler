plugins {
    java
    application
}

dependencies {
    // El debugger interpreta el AST de BBK: reusa el lexer/parser/AST de bbk-core.
    implementation(project(":bbk-core"))
    testImplementation("junit:junit:4.13.2")
    // End-to-end: traducir RPG con el frontend y ejecutar el BBK resultante.
    testImplementation(project(":rpg-frontend"))
}

application {
    mainClass.set("com.larena.boxbreaker.debugger.DebugCli")
}

tasks.test {
    useJUnit()
}
