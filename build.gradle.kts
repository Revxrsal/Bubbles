import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("java")
    id("com.vanniktech.maven.publish") version "0.29.0"
}

group = "io.github.revxrsal"
version = "1.3"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    compileOnly("org.ow2.asm:asm:9.7.1")
    compileOnly("org.ow2.asm:asm-commons:9.7.1")
    compileOnly("org.yaml:snakeyaml:2.0")
    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("org.jetbrains:annotations:26.0.1")
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

mavenPublishing {
    coordinates(
        groupId = group as String,
        artifactId = "bubbles",
        version = version as String
    )
    pom {
        name.set("Bubbles")
        description.set("A library for generating beautiful, commented, type-safe YML through interfaces")
        inceptionYear.set("2024")
        url.set("https://github.com/Revxrsal/Bubbles/")
        licenses {
            license {
                name.set("MIT")
                url.set("https://mit-license.org/")
                distribution.set("https://mit-license.org/")
            }
        }
        developers {
            developer {
                id.set("revxrsal")
                name.set("Revxrsal")
                url.set("https://github.com/Revxrsal/")
            }
        }
        scm {
            url.set("https://github.com/Revxrsal/Bubbles/")
            connection.set("scm:git:git://github.com/Revxrsal/Bubbles.git")
            developerConnection.set("scm:git:ssh://git@github.com/Revxrsal/Bubbles.git")
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

}