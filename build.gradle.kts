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
    implementation("org.apache.httpcomponents:httpclient:4.5.14") {
        exclude(group = "commons-codec")
    }
    implementation("com.jcraft:jsch:0.1.54")
    implementation("org.apache.logging.log4j:log4j-core:2.12.4")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.1.0.202411261347-r")
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")
}