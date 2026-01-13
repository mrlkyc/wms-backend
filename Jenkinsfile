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
        HOST_BACKEND_URL   = "http://localhost:8089"
        HOST_SELENIUM_URL  = "http://localhost:4444"

        DOCKER_BACKEND_URL  = "http://wms-backend:8080"
        DOCKER_SELENIUM_URL = "http://selenium-chrome:4444"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
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
                sleep(time: 30, unit: 'SECONDS')
            }
        }

        stage('E2E - Login') {
            steps {
                bat '''
                mvn test -Pe2e ^
                -Dtest=LoginE2ETest ^
                -Dspring.profiles.active=test ^
                -Dapp.url=%DOCKER_BACKEND_URL% ^
                -Dselenium.remote.url=%DOCKER_SELENIUM_URL%
                '''
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Reset Selenium') {
            steps {
                bat 'docker restart selenium-chrome'
                sleep(time: 15, unit: 'SECONDS')
            }
        }

        stage('E2E - Product Search') {
            steps {
                bat '''
                mvn test -Pe2e ^
                -Dtest=ProductSearchE2ETest ^
                -Dspring.profiles.active=test ^
                -Dapp.url=%DOCKER_BACKEND_URL% ^
                -Dselenium.remote.url=%DOCKER_SELENIUM_URL%
                '''
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('E2E - Product CRUD') {
            steps {
                bat '''
                mvn test -Pe2e ^
                -Dtest=ProductE2ETest ^
                -Dspring.profiles.active=test ^
                -Dapp.url=%DOCKER_BACKEND_URL% ^
                -Dselenium.remote.url=%DOCKER_SELENIUM_URL%
                '''
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

    } // ✅ stages KAPANDI

    post {
        always {
            bat 'docker-compose down -v'
        }
        success {
            echo 'PIPELINE BAŞARILI'
        }
        failure {
            echo 'PIPELINE BAŞARISIZ'
        }
    }

} // ✅ pipeline KAPANDI
