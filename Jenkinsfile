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
        // 🔴 E2E HARİCİ (host için)
        HOST_BACKEND_URL = "http://localhost:8089"
        HOST_SELENIUM_URL = "http://localhost:4444"

        // 🟢 E2E (Docker network içi)
        DOCKER_BACKEND_URL = "http://wms-backend:8080"
        DOCKER_SELENIUM_URL = "http://selenium-chrome:4444"
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
                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Integration Tests') {
            steps {
                bat 'mvn test -Dtest=*IntegrationTest'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Force Clean Docker') {
            steps {
                echo '🧹 Eski containerlar temizleniyor'
                bat '''
                docker rm -f wms-postgres || echo yok
                docker rm -f selenium-chrome || echo yok
                docker rm -f wms-backend || echo yok
                '''
            }
        }

        stage('Start System (Docker)') {
            steps {
                echo '🐳 Docker servisleri ayağa kaldırılıyor'
                bat '''
                docker-compose down -v
                docker-compose up -d
                '''
            }
        }

        stage('Wait for Services') {
            steps {
                echo '⏳ Backend ve Selenium ayağa kalkması bekleniyor'
                sleep(time: 30, unit: 'SECONDS')
            }
        }

        // ===================== E2E TESTLER =====================

        stage('E2E - Login') {
            steps {
                echo '🔐 E2E Login Testi'
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
        echo '♻ Selenium resetleniyor'
        bat '''
        docker restart selenium-chrome
        '''
        sleep(time: 15, unit: 'SECONDS')
    }
}

        stage('E2E - Login') {
            steps {
                echo '🔐 E2E Login Testi'
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
                echo '♻ Selenium resetleniyor'
                bat 'docker restart selenium-chrome'
                sleep(time: 15, unit: 'SECONDS')
            }
        }

        // 🔁 YERİ DEĞİŞTİ → SEARCH ÖNCE
        stage('E2E - Product Search') {
            when {
                expression {
                    return fileExists('src/test/java/com/wms/e2e/ProductSearchE2ETest.java')
                }
            }
            steps {
                echo '🔍 E2E Product Search Testi'
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

        // 🔁 YERİ DEĞİŞTİ → CRUD SONRA
        stage('E2E - Product CRUD') {
            steps {
                echo '📦 E2E Product CRUD Testi'
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

    post {
        always {
            echo '🧹 Docker ortamı kapatılıyor'
            bat 'docker-compose down -v'
        }
        success {
            echo '✅ PIPELINE BAŞARILI – TÜM AŞAMALAR GEÇTİ'
        }
        failure {
            echo '❌ PIPELINE BAŞARISIZ – LOG KONTROL EDİN'
        }
    }
}
