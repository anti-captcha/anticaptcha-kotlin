plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
    // Real API docs for the -javadoc.jar: the javadoc tool only reads .java files,
    // so on a Kotlin-only project it would produce an empty jar.
    id("org.jetbrains.dokka") version "2.2.0"
    id("org.jetbrains.dokka-javadoc") version "2.2.0"
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

kotlin {
    // Target Java 11 bytecode without requiring a JDK 11 toolchain to be installed:
    // -Xjdk-release also limits the JDK API surface, so nothing newer sneaks in.
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xjdk-release=11")
    }

    // Every declaration in the public API must state its visibility and return type
    explicitApi()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(11)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

java {
    withSourcesJar()
}

val javadocJar by tasks.registering(Jar::class) {
    group = "documentation"
    description = "Packs the Dokka output as the -javadoc.jar Maven Central expects"
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaGeneratePublicationJavadoc"))
}

/**
 * Runs one of the examples:
 *   ./gradlew runExample -Pexample=image
 */
val runExample by tasks.registering(JavaExec::class) {
    group = "application"
    description = "Runs an example from src/test/kotlin/com/anticaptcha/examples"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.anticaptcha.examples.ExamplesKt")
    args = listOfNotNull(project.findProperty("example")?.toString())
}

// ------------------------------------------------------------------ publishing

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "anticaptcha-kotlin"
            from(components["java"])
            artifact(javadocJar)

            pom {
                name.set("Anti-Captcha.com Kotlin client")
                description.set(
                    "Official Anti-Captcha.com Kotlin client with coroutines: image captchas, " +
                        "Recaptcha V2/V3 (Enterprise and non-Enterprise), hCaptcha, FunCaptcha " +
                        "(Arkose Labs), GeeTest v3/v4, Cloudflare Turnstile, Amazon WAF, Prosopo, " +
                        "Friendly Captcha, Altcha, AntiGate custom tasks and AntiBot cookies."
                )
                url.set("https://github.com/anti-captcha/anticaptcha-kotlin")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        name.set("Anti-Captcha.com")
                        email.set("support@anti-captcha.com")
                        organization.set("ANTICAPTCHA DEVELOPMENT LP")
                        organizationUrl.set("https://anti-captcha.com")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/anti-captcha/anticaptcha-kotlin.git")
                    developerConnection.set("scm:git:ssh://git@github.com/anti-captcha/anticaptcha-kotlin.git")
                    url.set("https://github.com/anti-captcha/anticaptcha-kotlin")
                }
            }
        }
    }

    repositories {
        // Everything lands here first, then gets zipped into a Central Portal bundle.
        maven {
            name = "centralStaging"
            url = uri(layout.buildDirectory.dir("central-staging"))
        }
    }
}

signing {
    // Only sign when a key is configured, so `gradle build` works without GPG.
    isRequired = gradle.taskGraph.hasTask("publishMavenPublicationToCentralStagingRepository")
    useGpgCmd()
    sign(publishing.publications["maven"])
}

/**
 * Packs the staged artifacts into the zip Central Portal expects.
 * See maven_instructions.md in the anticaptcha-java repository for the upload step.
 */
val centralBundle by tasks.registering(Zip::class) {
    dependsOn("publishMavenPublicationToCentralStagingRepository")

    from(layout.buildDirectory.dir("central-staging")) {
        // Central rejects a bundle that carries repository metadata
        exclude("**/maven-metadata.xml*")
    }
    archiveFileName.set("central-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("central-publishing"))

    doLast {
        logger.lifecycle("Bundle ready: ${archiveFile.get().asFile}")
        logger.lifecycle("Upload it at https://central.sonatype.com/publishing or with:")
        logger.lifecycle(
            "  curl -H \"Authorization: Bearer \$(printf '%s:%s' \"\$CENTRAL_USERNAME\" " +
                "\"\$CENTRAL_PASSWORD\" | base64)\" \\"
        )
        logger.lifecycle(
            "       -F bundle=@${archiveFile.get().asFile} " +
                "\"https://central.sonatype.com/api/v1/publisher/upload?publishingType=AUTOMATIC\""
        )
    }
}
