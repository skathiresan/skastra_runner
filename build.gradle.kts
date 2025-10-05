plugins {
    id("java-library")
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    group = "com.acme.astra"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
        
        // Add your Artifactory repository here (only if credentials are available)
        if (System.getenv("ARTIFACTORY_USER") != null && System.getenv("ARTIFACTORY_PASSWORD") != null) {
            maven {
                name = "Artifactory"
                url = uri(System.getenv("ARTIFACTORY_URL") ?: "https://your-artifactory.com/artifactory/libs-release")
                credentials {
                    username = System.getenv("ARTIFACTORY_USER")
                    password = System.getenv("ARTIFACTORY_PASSWORD")
                }
            }
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.spring.dependency-management")
    
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
    
    dependencies {
        implementation("org.slf4j:slf4j-api:2.0.9")
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        testImplementation("org.mockito:mockito-core:5.7.0")
        testImplementation("org.assertj:assertj-core:3.24.2")
    }
    
    tasks.test {
        useJUnitPlatform()
    }
}