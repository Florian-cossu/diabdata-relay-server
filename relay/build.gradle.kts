plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    application
}

group = "com.diabdata"
version = "0.1.0"

application {
    mainClass.set("com.diabdata.relay.ApplicationKt")
}

repositories {
    mavenCentral()
}

val ktor_version = "3.1.3"

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")

    // Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // Tests
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.20")
}