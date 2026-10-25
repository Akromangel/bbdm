plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.dependency.updates)
    alias(libs.plugins.tabletest.reporter)
    pmd
    checkstyle
    jacoco
}

group = "kuchma.mike.bbdm"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation(libs.jspecify)

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.tabletest.junit)
    testImplementation(libs.wiremock)
}

tasks.withType<JavaCompile> {
    options.release = libs.versions.java.get().toInt()
    options.compilerArgs.add("-Xlint:all")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

pmd {
    isConsoleOutput = true
    toolVersion = libs.versions.pmd.get()
    ruleSetFiles = files("config/pmd/ruleset.xml")
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
}

tasks.withType<Checkstyle> {
    configFile = layout.projectDirectory.file("config/checkstyle/checkstyle.xml").asFile
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}
