plugins {
    id("java")
}

group = "io.github.revxrsal"
version = "1.0"

repositories {
    mavenCentral()
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

