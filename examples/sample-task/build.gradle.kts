plugins {
    id("java-library")
    id("maven-publish")
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":contracts"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

// Create executable JAR for CLI mode testing
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.sample.SampleTaskCLI"
    }
    
    // Include dependencies in the JAR
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    
    // Ensure contracts project is built first
    dependsOn(":contracts:jar")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}