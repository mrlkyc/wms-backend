pipeline {
    agent any

    tools {
        maven 'Maven-3.9.6'
        jdk  'jdk-21'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        BACKEND_URL  = "http://localhost:8089"
        SELENIUM_URL = "http://localhost:4444"
    }

    stages {

        stage('Checkout') {
            steps {
                echo '📥 GitHub’dan kodlar çekiliyor'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo '🔨 Proje build ediliyor (testsiz)'
                bat 'mvn clean package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Unit Tests') {
            steps {
                bat 'mvn test -Dtest=*ServiceTest'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Integration Tests') {
            steps {
                bat 'mvn test -Dtest=*IntegrationTest'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Force Clean Docker') {
            steps {
                bat '''
                docker rm -f wms-postgres || echo yok
                docker rm -f selenium-chrome || echo yok
                docker rm -f wms-backend || echo yok
                '''
            }
        }

        stage('Start System (Docker)') {
            steps {
                bat '''
                docker-compose down -v
                docker-compose up -d
                '''
            }
        }

        stage('Wait for Services') {
            steps {
                powershell '''
                $i=0
                while ($i -lt 30) {
                    try {
                        Invoke-WebRequest "$env:BACKEND_URL/actuator/health" -TimeoutSec 2
                        exit 0
                    } catch {}
                    Start-Sleep 5
                    $i++
                }
                exit 1
                '''
            }
        }

        stage('E2E - Login') {
            steps {
                bat 'mvn test -Pe2e -Dtest=LoginE2ETest'
            }
            post {
                always { junit 'target/surefire-reports/*.xml' }
            }
        }

        stage('E2E - Logout') {
            steps {
                bat 'mvn test -Pe2e -Dtest=LogoutE2ETest'
            }
            post {
                always { junit 'target/surefire-reports/*.xml' }
            }
        }

        stage('E2E - Product CRUD') {
            steps {
                bat 'mvn test -Pe2e -Dtest=ProductE2ETest'
            }
            post {
                always { junit 'target/surefire-reports/*.xml' }
            }
        }

        stage('E2E - Product Search') {
            steps {
                bat 'mvn test -Pe2e -Dtest=ProductSearchE2ETest'
            }
            post {
                always { junit 'target/surefire-reports/*.xml' }
            }
        }
    }

    post {
        always {
            bat 'docker-compose down -v'
        }
        success {
            echo '✅ PIPELINE BAŞARILI'
        }
        failure {
            echo '❌ PIPELINE BAŞARISIZ'
        }
    }
}
