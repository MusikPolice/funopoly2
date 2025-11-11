group = "ca.jonathanfritz"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "2.2.20"
    application
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.20")

    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:6.0.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    targetCompatibility = "24"
}

application {
    mainClass.set("ca.jonathanfritz.monopoly.MonopolyKt")
}
