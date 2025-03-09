plugins {
    kotlin("jvm") version "2.1.0"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

group = "org.tera201"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-io:commons-io:2.14.0")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    implementation("org.apache.commons:commons-lang3:3.3.2")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.1.0.202411261347-r"){
        exclude(group = "org.slf4j")
    }
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")
}