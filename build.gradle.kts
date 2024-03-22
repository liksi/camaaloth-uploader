import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
    id("com.vaadin") version "24.3.7"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
}

group = "org.breizhcamp.camaaloth-uploader"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

extra["vaadinVersion"] = "24.3.7"
extra["karibuDSlVersion"] = "2.1.2"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("com.vaadin:vaadin-spring-boot-starter")
    compileOnly("com.github.mvysny.karibudsl:karibu-dsl:${properties["karibuDSlVersion"]}")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")

    implementation("org.apache.commons:commons-lang3")
    implementation("commons-io:commons-io:2.15.1")
    implementation("org.webjars:bootstrap:3.3.7-1")
    implementation("org.webjars:sockjs-client:1.1.2")
    implementation("org.webjars:stomp-websocket:2.3.3-1")
    implementation("org.webjars:angularjs:1.6.2")
    implementation("com.google.apis:google-api-services-youtube:v3-rev182-1.22.0")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.willowtreeapps.assertk:assertk:0.27.0")
    testImplementation("net.sourceforge.htmlunit:htmlunit")
    testImplementation("com.microsoft.playwright:playwright:1.36.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

dependencyManagement {
    imports {
        mavenBom("com.vaadin:vaadin-bom:${property("vaadinVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}


tasks.withType<Test> {
    useJUnitPlatform()
}

