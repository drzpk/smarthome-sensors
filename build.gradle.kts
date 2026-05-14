import com.google.cloud.tools.jib.api.Jib
import com.google.cloud.tools.jib.gradle.JibTask

plugins {
    application
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.allopen") version "2.2.20"
    id("com.google.cloud.tools.jib") version "3.4.5"
}

group = "dev.drzepka.smarthome"
version = "1.2.1"

val logback_version: String by project
val ktor_version: String by project
val kotlin_version: String by project


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
    val koinVersion: String by project
    val exposedVersion: String by project

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("io.ktor:ktor-server-tomcat:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-sessions:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.liquibase:liquibase-core:4.33.0")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.0")
    implementation("com.influxdb:influxdb-client-kotlin:7.3.0")
    implementation("com.typesafe:config:1.4.1")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.5.6")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.47.0.0")
    testImplementation("org.assertj:assertj-core:3.19.0")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("org.mockito:mockito-core:5.19.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.19.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.0.0")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    val testcontainersVersion = "2.0.4"
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers-influxdb:$testcontainersVersion")
    // Required at compile time: Testcontainers' GenericContainer implements JUnit 4's TestRule
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
        image = "registry.gitlab.com/smart-home-dr/sensors/sensors"
        tags = setOf(project.version as String)
        auth {
            username = getContainerRegistryUser()
            password = getContainerRegistryPassword()
        }
    }
    container {
        entrypoint = listOf("sh",
            "-c",
            """
                |java 
                |-cp `cat /app/jib-classpath-file`
                |-Dlogback.configurationFile=/app/config/logback.xml 
                |-DEXTERNAL_CONFIG_PATH=/app/config/application.conf 
                |${'$'}JAVA_OPTS 
                |io.ktor.server.tomcat.EngineMain""".trimMargin().lines().joinToString(" ")
        )

        environment = mapOf(
            "JAVA_OPTS" to ""
        )
        creationTime = "USE_CURRENT_TIMESTAMP"
        workingDirectory = "/app"
        labels.put("Maintainer", "dominik.1.rzepka@gmail.com")
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

fun getContainerRegistryUser(): String {
    val user = System.getenv("CI_DEPLOY_USER")
    if (user != null)
        return user

    return "d_rzepka"
}

fun getContainerRegistryPassword(): String {
    val ciToken = System.getenv("CI_DEPLOY_PASSWORD")
    if (ciToken != null)
        return ciToken

    // from ~/.gradle/gradle.properties
    val privateToken = findProperty("gitLabPrivateToken") as String?
    return if (privateToken == null) {
        logger.warn("Container registry token is missing, publishing will fail")
        ""
    } else {
        privateToken
    }
}