import com.google.cloud.tools.jib.api.buildplan.ImageFormat
import com.google.cloud.tools.jib.gradle.JibTask

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.jib)
}

group = "dev.drzepka.smarthome"
version = "1.3.0"

application {
    mainClass.set("io.ktor.server.tomcat.EngineMain")
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.ktor.server.tomcat)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.sessions)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.exposed.core)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.hikari)
    implementation(libs.liquibase)
    implementation(libs.logback)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.influxdb.client.kotlin)
    implementation(libs.typesafe.config)
    runtimeOnly(libs.mariadb)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.sqlite.jdbc)
    testImplementation(libs.assertj.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.koin.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.influxdb)
}

allOpen {
    annotation("dev.drzepka.smarthome.sensors.server.domain.util.Mockable")
}

configurations {
    all {
        exclude(group = "junit")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JibTask> {
    dependsOn(tasks.named("build"))
}

jib {
    from {
        image = "eclipse-temurin:21-jre"
    }
    to {
        image = getContainerRegistryImage()
        tags = setOf(project.version as String)
        val registryUser = System.getenv("CI_DEPLOY_USER")
        val registryPassword = System.getenv("CI_DEPLOY_PASSWORD")
        if (registryUser != null && registryPassword != null) {
            auth {
                username = registryUser
                password = registryPassword
            }
        }
    }
    container {
        mainClass = "io.ktor.server.tomcat.jakarta.EngineMain"
        format = ImageFormat.OCI
        creationTime = "USE_CURRENT_TIMESTAMP"
        workingDirectory = "/app"
        labels.put("Maintainer", "dominik.1.rzepka@gmail.com")
        jvmFlags = listOf("-Dlogback.configurationFile=\${LOGBACK_CONFIG_FILE}")
    }
    extraDirectories {
        paths {
            path {
                setFrom(File(layout.buildDirectory.get().asFile, "libs"))
                into = "/app"
            }
        }
    }
}

fun getContainerRegistryImage(): String {
    return System.getenv("CONTAINER_REGISTRY_IMAGE")
        ?: "registry.gitlab.com/smart-home-dr/sensors/sensors"
}
