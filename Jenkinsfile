pipeline {
    agent {
        label 'jdk21' // Ensure agent has JDK 21
    }
    
    parameters {
        string(name: 'GA', defaultValue: 'com.example:sample-task', description: 'Group:Artifact to execute')
        string(name: 'VERSION', defaultValue: 'latest.release', description: 'Version to execute')
        choice(name: 'MODE', choices: ['CLI', 'SPI'], description: 'Execution mode')
        string(name: 'ARGS_JSON', defaultValue: '{}', description: 'Runtime arguments as JSON')
        string(name: 'REPORTS_DIR', defaultValue: 'reports', description: 'Reports output directory')
        string(name: 'TIMEOUT_MS', defaultValue: '300000', description: 'Execution timeout in milliseconds')
    }
    
    environment {
        GRADLE_OPTS = '-Dorg.gradle.daemon=false -Dorg.gradle.parallel=false'
        // Artifactory credentials should be configured in Jenkins credentials store
        ARTIFACTORY_USER = credentials('artifactory-user')
        ARTIFACTORY_PASSWORD = credentials('artifactory-password')
        ARTIFACTORY_URL = credentials('artifactory-url')
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build') {
            steps {
                script {
                    echo "Building Astra Runner..."
                    sh './gradlew clean build -x test'
                }
            }
        }
        
        stage('Test') {
            steps {
                script {
                    echo "Running unit tests..."
                    sh './gradlew test'
                }
            }
            post {
                always {
                    publishTestResults(testResultsPattern: '**/build/test-results/test/*.xml')
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'build/reports/tests/test',
                        reportFiles: 'index.html',
                        reportName: 'Unit Test Report'
                    ])
                }
            }
        }
        
        stage('Package') {
            steps {
                script {
                    echo "Packaging application..."
                    sh './gradlew bootJar'
                }
            }
        }
        
        stage('Execute Task') {
            steps {
                script {
                    echo "Executing task ${params.GA}:${params.VERSION} in ${params.MODE} mode"
                    
                    // Create reports directory
                    sh "mkdir -p ${params.REPORTS_DIR}"
                    
                    // Execute the task
                    def exitCode = sh(
                        script: """
                            java -jar app/build/libs/astra-runner.jar \\
                                --ga="${params.GA}" \\
                                --version="${params.VERSION}" \\
                                --mode="${params.MODE}" \\
                                --argsJson='${params.ARGS_JSON}' \\
                                --reportsDir="${params.REPORTS_DIR}" \\
                                --timeout="${params.TIMEOUT_MS}" \\
                                --workspace="./workspace"
                        """,
                        returnStatus: true
                    )
                    
                    echo "Task execution completed with exit code: ${exitCode}"
                    
                    // Store exit code for later use
                    env.TASK_EXIT_CODE = exitCode.toString()
                }
            }
            post {
                always {
                    // Archive all reports
                    archiveArtifacts(
                        artifacts: "${params.REPORTS_DIR}/**/*",
                        allowEmptyArchive: true,
                        fingerprint: true
                    )
                    
                    // Publish JUnit results if available
                    script {
                        if (fileExists("${params.REPORTS_DIR}/junit.xml")) {
                            publishTestResults(testResultsPattern: "${params.REPORTS_DIR}/junit.xml")
                        }
                    }
                    
                    // Publish HTML summary if available
                    script {
                        if (fileExists("${params.REPORTS_DIR}/summary.html")) {
                            publishHTML([
                                allowMissing: false,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: params.REPORTS_DIR,
                                reportFiles: 'summary.html',
                                reportName: 'Execution Summary'
                            ])
                        }
                    }
                }
            }
        }
        
        stage('Analyze Results') {
            steps {
                script {
                    echo "Analyzing execution results..."
                    
                    // Read and display summary
                    if (fileExists("${params.REPORTS_DIR}/run-summary.json")) {
                        def summary = readJSON file: "${params.REPORTS_DIR}/run-summary.json"
                        echo "Execution Summary:"
                        echo "- Execution ID: ${summary.executionId}"
                        echo "- Status: ${summary.executionResult?.status}"
                        echo "- Duration: ${summary.executionResult?.durationMs}ms"
                        echo "- Message: ${summary.executionResult?.message}"
                        
                        // Set build description
                        currentBuild.description = "Executed ${params.GA}:${params.VERSION} - ${summary.executionResult?.status}"
                    }
                    
                    // Check if execution was successful
                    if (env.TASK_EXIT_CODE != '0') {
                        currentBuild.result = 'UNSTABLE'
                        echo "Task execution failed with exit code: ${env.TASK_EXIT_CODE}"
                    } else {
                        echo "Task execution completed successfully"
                    }
                }
            }
        }
    }
    
    post {
        always {
            // Clean up workspace files but keep reports
            sh 'rm -rf workspace || true'
            
            // Send notifications
            script {
                def status = currentBuild.result ?: 'SUCCESS'
                def color = status == 'SUCCESS' ? 'good' : 'warning'
                
                echo "Pipeline completed with status: ${status}"
                
                // You can add Slack, email, or other notifications here
                // Example:
                // slackSend(
                //     channel: '#ci-cd',
                //     color: color,
                //     message: "Astra Runner execution ${status}: ${params.GA}:${params.VERSION} - ${env.BUILD_URL}"
                // )
            }
        }
        
        success {
            echo "Pipeline completed successfully!"
        }
        
        failure {
            echo "Pipeline failed!"
        }
        
        unstable {
            echo "Pipeline completed but task execution had issues"
        }
    }
}