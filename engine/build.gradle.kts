dependencies {
    api(project(":contracts"))
    
    implementation("org.springframework:spring-context:6.1.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.0")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("org.apache.commons:commons-compress:1.24.0")
    implementation("commons-io:commons-io:2.15.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // For HTTP client (Artifactory REST API)
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")
    
    // For checksum calculation
    implementation("commons-codec:commons-codec:1.16.0")
}