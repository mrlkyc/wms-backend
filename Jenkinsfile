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
        BACKEND_URL  = "http://wms-backend:8089"
        SELENIUM_URL = "http://selenium-chrome:4444"
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
                echo '⏳ Backend container HEALTHY mi kontrol ediliyor'

                powershell '''
                $maxRetry = 30
                $retry = 0

                while ($retry -lt $maxRetry) {
                    $status = docker inspect -f "{{.State.Health.Status}}" wms-backend 2>$null

                    if ($status -eq "healthy") {
                        Write-Host "✅ Backend HEALTHY"
                        exit 0
                    }

                    Write-Host "⏳ Backend bekleniyor... ($status)"
                    Start-Sleep -Seconds 5
                    $retry++
                }

                Write-Error "❌ Backend HEALTHY olmadı"
                exit 1
                '''
            }
        }

        stage('E2E - Login') {
            steps {
                bat 'mvn test -Pe2e -Dtest=LoginE2ETest'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }




        stage('E2E - Product CRUD') {
            steps {
                bat 'mvn test -Pe2e -Dtest=ProductE2ETest'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('E2E - Product Search') {
            steps {
                bat 'mvn test -Pe2e -Dtest=ProductSearchE2ETest'
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
