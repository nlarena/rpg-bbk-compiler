plugins {
    java
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// NOTE: Spring Initializr generates a `java { toolchain { languageVersion = of(21) } }`
// block here. We omit it on purpose: this is a monorepo subproject and the root
// build.gradle.kts already pins every Java module to release 21, so the build works on
// the installed JDK (25) without requiring a separate JDK 21 toolchain.
