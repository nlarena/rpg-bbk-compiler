plugins {
    java
    application
}

dependencies {
    // ASM: generate JVM .class bytecode for the bytecode backend.
    implementation("org.ow2.asm:asm:9.7")
    testImplementation("junit:junit:4.13.2")
}

application {
    mainClass.set("com.larena.boxbreaker.core.BbkCli")
}

tasks.test {
    useJUnit()
}
