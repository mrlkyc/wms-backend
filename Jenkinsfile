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
            post {
                success { echo '✅ Checkout başarılı' }
                failure { echo '❌ Checkout başarısız' }
            }
        }

        stage('Build (Skip Tests)') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
            post {
                success { echo '✅ Build başarılı' }
                failure { echo '❌ Build başarısız' }
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
                success { echo '✅ Unit testler geçti' }
                failure { echo '❌ Unit test hatası' }
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
                success { echo '✅ Integration testler geçti' }
                failure { echo '❌ Integration test hatası' }
            }
        }

        stage('Start Docker System') {
            steps {
                bat '''
                docker-compose down -v
                docker-compose up -d
                '''
            }
            post {
                success { echo '🐳 Docker sistemi ayakta' }
                failure { echo '❌ Docker başlatılamadı' }
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
            post {
                success { echo '🟢 Selenium hazır' }
                failure { echo '🔴 Selenium hazır değil' }
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
                success { echo '✅ Login page load testi geçti' }
                failure { echo '❌ Login page load testi başarısız' }
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
                success { echo '✅ Valid login testi geçti' }
                failure { echo '❌ Valid login testi başarısız' }
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
                success { echo '✅ Invalid login testi geçti' }
                failure { echo '❌ Invalid login testi başarısız' }
            }
        }
    }

    post {
        always {
            echo '🧹 Docker ortamı kapatılıyor'
            bat 'docker-compose down -v'
        }

        success {
            echo '''
=============================
✅ PIPELINE BAŞARILI
✔ Build
✔ Unit Tests
✔ Integration Tests
✔ Login E2E (3/3)
=============================
'''
        }

        failure {
            echo '''
=============================
❌ PIPELINE BAŞARISIZ
⛔ Bir veya daha fazla stage hata aldı
📄 Test raporlarını inceleyin
=============================
'''
        }
    }
}
