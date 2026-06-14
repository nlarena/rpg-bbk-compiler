plugins {
    java
    application
}

dependencies {
    implementation(project(":rpg-frontend"))
    testImplementation("junit:junit:4.13.2")
}

application {
    mainClass.set("com.larena.boxbreaker.debugger.TranslationView")
}

tasks.test {
    useJUnit()
}
