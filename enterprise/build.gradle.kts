import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.jpa") version "1.9.22"
    kotlin("kapt") version "1.9.22"
}

group = "com.obsidianbackup"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    
    // SAML SSO (Finding 1)
    implementation("org.springframework.security:spring-security-saml2-service-provider")
    
    // JWT Authentication (Finding 6)
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")
    
    // PostgreSQL (Finding 5)
    runtimeOnly("org.postgresql:postgresql")
    
    // Database Migration (Flyway)
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    
    // Kotlin Support
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
    
    // Circuit Breaker (Finding 10 - valuable context)
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    
    // Distributed Tracing (Finding 10 - valuable context)
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.reporter2:zipkin-reporter-brave")
    
    // Firebase Cloud Messaging (Finding 3)
    implementation("com.google.firebase:firebase-admin:9.2.0")
    
    // Redis for JWT Blacklist (Finding 6)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.session:spring-session-data-redis")
    
    // Monitoring & Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // Development Tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.testcontainers:testcontainers:1.19.4")
    testImplementation("org.testcontainers:postgresql:1.19.4")
    testImplementation("org.testcontainers:junit-jupiter:1.19.4")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    
    // Annotation Processing
    kapt("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Flyway configuration
flyway {
    url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/obsidian_enterprise"
    user = System.getenv("DATABASE_USER") ?: "obsidian_ent"
    password = System.getenv("DATABASE_PASSWORD") ?: "dev_password_change_in_production"
    schemas = arrayOf("public")
    locations = arrayOf("classpath:db/migration")
    baselineOnMigrate = true
    validateOnMigrate = true
}
