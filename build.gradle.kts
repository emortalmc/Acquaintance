import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    `maven-publish`

    java
}

repositories {
    mavenCentral()

    maven(url = "https://jitpack.io")
    maven(url = "https://repo.spongepowered.org/maven")
}

dependencies {
    //compileOnly(kotlin("stdlib"))

    compileOnly("com.github.Minestom:Minestom:0db5993bfb")
    compileOnly("com.github.EmortalMC:Immortal:5cdd2d9dd3")
    //compileOnly("com.github.emortaldev:Kstom:def1719826")
    //compileOnly("net.luckperms:api:5.3")

    compileOnly("org.redisson:redisson:3.17.7")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    compileOnly("mysql:mysql-connector-java:8.0.31")
    compileOnly("com.zaxxer:HikariCP:5.0.1")

    // import kotlinx serialization
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
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
        minimize {
            exclude(dependency("mysql:mysql-connector-java:8.0.31"))
        }
    }

    // Make build depend on shadowJar as shading dependencies will most likely be required.
    build { dependsOn(shadowJar) }

    shadowJar {
        relocate("com.zaxxer.hikari", "dev.emortal.datadependency.libs.hikari")
    }

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