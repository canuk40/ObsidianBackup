plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.serialization") version "1.9.25"
    id("io.ktor.plugin") version "2.3.7"
    application
}

group = "com.obsidianbackup.enterprise"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("io.ktor:ktor-server-auth:2.3.7")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.7")
    implementation("io.ktor:ktor-server-cors:2.3.7")
    implementation("io.ktor:ktor-server-status-pages:2.3.7")
    implementation("io.ktor:ktor-server-call-logging:2.3.7")
    
    // SAML Support
    implementation("com.onelogin:java-saml:2.9.0")
    
    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.57.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.57.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.57.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.57.0")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    
    // Encryption
    implementation("org.bouncycastle:bcprov-jdk15on:1.79")

    // PDF generation (M-13)
    implementation("org.apache.pdfbox:pdfbox:3.0.3")
    
    // Testing
    testImplementation("io.ktor:ktor-server-test-host:2.3.7")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.25")
    testImplementation("io.mockk:mockk:1.13.14")
}

application {
    mainClass.set("com.obsidianbackup.enterprise.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
