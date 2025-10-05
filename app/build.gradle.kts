plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    application
}

dependencies {
    implementation(project(":contracts"))
    implementation(project(":engine"))
    
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("info.picocli:picocli:4.7.5")
    implementation("info.picocli:picocli-spring-boot-starter:4.7.5")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

application {
    mainClass.set("com.acme.astra.app.AstraRunnerApplication")
}

tasks.bootJar {
    archiveFileName.set("astra-runner.jar")
}