plugins {
    id("java")
}

group = "dev.lussuria"
version = "1.0.0"

repositories {
    mavenCentral()
}

val hytaleServerInput = (findProperty("hytaleServerJar") as String?)
    ?: (findProperty("HytaleServerJar") as String?)
    ?: System.getenv("HYTALE_SERVER_JAR")
    ?: System.getenv("HytaleServerJar")

val hytaleServerJar = hytaleServerInput?.let { input ->
    val path = file(input)
    when {
        path.isFile && path.extension.equals("jar", ignoreCase = true) -> path
        path.isDirectory -> path.listFiles()
            ?.firstOrNull { it.isFile && it.extension.equals("jar", ignoreCase = true) }
        else -> null
    }
}

dependencies {
    if (hytaleServerJar != null) {
        compileOnly(files(hytaleServerJar))
    } else {
        logger.warn(
            "HYTALE_SERVER_JAR or -PhytaleServerJar is not set (or did not resolve to a .jar). " +
                "Compilation will fail without the Hytale server API jar."
        )
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
