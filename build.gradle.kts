import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.22"
    kotlin("plugin.serialization") version "1.8.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`

    java
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    //compileOnly(kotlin("stdlib"))

    compileOnly("com.github.hollow-cube:Minestom:e6d4a2cc91")
    compileOnly("com.github.EmortalMC:Immortal:30f89199e2")
    //compileOnly("com.github.emortaldev:Kstom:def1719826")
    //compileOnly("net.luckperms:api:5.3")

    compileOnly("redis.clients:jedis:4.3.2")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    compileOnly("org.litote.kmongo:kmongo-coroutine-serialization:4.9.0")

    // import kotlinx serialization
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}

// Take gradle.properties and apply it to resources.
tasks {
    processResources {
        // Apply properties to extension.json
        filesMatching("extension.json") {
            expand(project.properties)
        }
    }

    // Set name, minimize, and merge service files
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set(project.name)
        mergeServiceFiles()
        minimize()
    }

    // Make build depend on shadowJar as shading dependencies will most likely be required.
    build { dependsOn(shadowJar) }

}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val compileKotlin: KotlinCompile by tasks
tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "17" }

compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.properties["group"] as? String?
            artifactId = project.name
            version = project.properties["version"] as? String?

            from(components["java"])
        }
    }
}