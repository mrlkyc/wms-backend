pipeline {
    agent any

    tools {
        maven 'Maven-3.9.6'
        jdk 'jdk-21'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        DOCKER_BACKEND_URL  = "http://wms-backend:8080"
        DOCKER_SELENIUM_URL = "http://selenium-chrome:4444"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build (Skip Tests)') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Unit Tests') {
            steps {
                bat 'mvn test -Dtest=*ServiceTest'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Integration Tests') {
            steps {
                bat 'mvn test -Dtest=*IntegrationTest'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Start Docker System') {
            steps {
                bat '''
                docker-compose down -v
                docker-compose up -d
                '''
            }
        }

        stage('Wait for Selenium') {
            steps {
                bat '''
                echo Selenium bekleniyor...
                for /L %%i in (1,1,15) do (
                    docker exec selenium-chrome curl -s http://localhost:4444/status && exit /b 0
                    timeout /t 4
                )
                exit /b 1
                '''
            }
        }

        // ================= LOGIN E2E =================

        stage('E2E - Login Page Loads') {
            steps {
                bat '''
                mvn test -Pe2e ^
                -Dtest=LoginE2ETest#loginPage_shouldLoad ^
                -Dselenium.remote.url=%DOCKER_SELENIUM_URL%
                '''
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('E2E - Valid Login Redirects') {
            steps {
                bat '''
                mvn test -Pe2e ^
                -Dtest=LoginE2ETest#validLogin_shouldRedirectToAdminPage ^
                -Dselenium.remote.url=%DOCKER_SELENIUM_URL%
                '''
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('E2E - Invalid Login Shows Error') {
            steps {
                bat '''
                mvn test -Pe2e ^
                -Dtest=LoginE2ETest#invalidLogin_shouldShowErrorMessage ^
                -Dselenium.remote.url=%DOCKER_SELENIUM_URL%
                '''
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
    }

    post {
        always {
            bat 'docker-compose down -v'
        }
        success {
            echo '✅ PIPELINE BAŞARILI – LOGIN E2E TAMAMLANDI'
        }
        failure {
            echo '❌ PIPELINE BAŞARISIZ'
        }
    }
}
