plugins {
    kotlin("jvm") version "1.8.20"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

group = "org.tera201"
version = "0.3.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/tera201/SWRMiner")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("commons-io:commons-io:2.8.0")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    implementation("org.apache.commons:commons-lang3:3.3.2")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("com.jcraft:jsch:0.1.54")
    implementation("org.apache.logging.log4j:log4j-core:2.12.4")
    implementation("org.eclipse.jgit:org.eclipse.jgit:4.8.0.201706111038-r") {
        exclude(group = "org.slf4j")
        exclude(group = "commons-codec")
        exclude(group = "org.apache.httpcomponents")
    }
    implementation("org.slf4j:slf4j-api:1.7.2")
    implementation("org.slf4j:slf4j-simple:1.7.2")
    implementation("org.tmatesoft.svnkit:svnkit:1.8.10")
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.1")
}