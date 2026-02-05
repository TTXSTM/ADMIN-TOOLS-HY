plugins {
    id("java")
}

group = "dev.lussuria"
version = "1.0.0"

repositories {
    mavenCentral()
}

val hytaleServerJar = (findProperty("hytaleServerJar") as String?)
    ?: System.getenv("HYTALE_SERVER_JAR")

dependencies {
    if (hytaleServerJar != null) {
        compileOnly(files(hytaleServerJar))
    } else {
        logger.warn("HYTALE_SERVER_JAR or -PhytaleServerJar is not set. Compilation will fail without the Hytale server API jar.")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
}
