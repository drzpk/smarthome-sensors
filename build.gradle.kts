plugins {
    application
    kotlin("jvm") version "1.5.20"
    kotlin("plugin.allopen") version "1.5.20"
    id("com.google.cloud.tools.jib") version "3.1.2"
}

group = "dev.drzepka.smarthome"
version = "1.0.0-SNAPSHOT"

val logback_version: String by project
val ktor_version: String by project
val kotlin_version: String by project


application {
    mainClassName = "io.ktor.server.tomcat.EngineMain"
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

dependencies {
    val koinVersion: String by project
    val exposedVersion: String by project

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("io.ktor:ktor-server-tomcat:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-sessions:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    implementation("io.ktor:ktor-auth:$ktor_version")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("mysql:mysql-connector-java:8.0.19")
    implementation("com.zaxxer:HikariCP:3.4.2")
    implementation("org.liquibase:liquibase-core:4.3.2")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.2")
    implementation("com.influxdb:influxdb-client-kotlin:2.3.0")
    implementation("com.typesafe:config:1.4.1")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testRuntimeOnly("com.h2database:h2:1.3.176")
    testImplementation("org.assertj:assertj-core:3.19.0")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("org.mockito:mockito-core:3.9.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.9.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.1.0")
}

allOpen {
    annotation("dev.drzepka.smarthome.sensors.server.domain.util.Mockable")
}

configurations {
    all {
        exclude(group = "junit")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jib {
    from {
        image = "openjdk:8-jre-slim-buster"
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
                setFrom(File(buildDir, "libs"))
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