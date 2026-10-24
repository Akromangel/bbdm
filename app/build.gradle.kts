import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    java
    id("jvm-test-suite")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.dependency.updates)
    alias(libs.plugins.tabletest.reporter)
    alias(libs.plugins.spotbugs)
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

val javaRuntimeImage = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
}.map { launcher -> launcher.metadata.installationPath.asFile.resolve("lib/jrt-fs.jar") }

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation(libs.jspecify)

    add("pmdAux", files(javaRuntimeImage))

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

testing {
    suites {
        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
            }
            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(tasks.test)
                    }
                }
            }
        }
    }
}

tasks.check {
    dependsOn(testing.suites.named("integrationTest"))
}

configurations.named("integrationTestImplementation") {
    extendsFrom(configurations.testImplementation.get())
}
configurations.named("integrationTestRuntimeOnly") {
    extendsFrom(configurations.testRuntimeOnly.get())
}

pmd {
    isConsoleOutput = true
    toolVersion = libs.versions.pmd.get()
    ruleSetFiles = files("config/pmd/ruleset.xml")
    isIgnoreFailures = false
}

tasks.withType<Pmd>().configureEach {
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt())
    }
}

tasks.named<Pmd>("pmdTest") {
    ruleSetFiles = files("config/pmd/test-ruleset.xml")
}

tasks.named<Pmd>("pmdIntegrationTest") {
    ruleSetFiles = files("config/pmd/test-ruleset.xml")
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    isIgnoreFailures = false
    isShowViolations = true
}

tasks.withType<Checkstyle>().configureEach {
    configFile = layout.projectDirectory.file("config/checkstyle/checkstyle.xml").asFile
    maxWarnings = 0
    reports {
        xml.required = true
        html.required = true
    }
}

spotbugs {
    toolVersion.set(libs.versions.spotbugs.engine.get())
    ignoreFailures = false
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("html") {
        required.set(true)
    }
    reports.create("xml") {
        required.set(true)
    }
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

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    revision = "release"
    rejectPreReleases = true
    rejectVersionIf {
        candidate.version.contains(Regex("(?i)(?:^|[.+_-])(?:snapshot|milestone|rc\\d*|m\\d*)(?:$|[.+_-])"))
    }
    outputFormatter = "json"
}
