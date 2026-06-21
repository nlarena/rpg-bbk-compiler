plugins {
    `java-library`
}

dependencies {
    // Depura el bytecode real de BBK: necesita compilarlo a .class (bbk-core) y luego
    // forkear una JVM con el agente JDWP para conectarse por JDI.
    implementation(project(":bbk-core"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}
