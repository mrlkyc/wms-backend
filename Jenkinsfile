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
        BACKEND_URL = "http://localhost:8089"
        SELENIUM_URL = "http://localhost:4444"
    }

    stages {

        // =================================================
        // 1. KODLARI ÇEK
        // =================================================
        stage('Checkout') {
            steps {
                echo '📥 GitHub’dan kodlar çekiliyor'
                checkout scm
            }
        }

        // =================================================
        // 2. BUILD (TESTSIZ)
        // =================================================
        stage('Build') {
            steps {
                echo '🔨 Proje build ediliyor (testsiz)'
                bat 'mvn clean package -DskipTests'
            }
            post {
                success {
                    echo '✅ Build başarılı'
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
                failure {
                    error '❌ Build başarısız'
                }
            }
        }

        // =================================================
        // 3. UNIT + INTEGRATION TESTLER
        // =================================================
        stage('Unit & Integration Tests') {
            steps {
                echo '🧪 Unit + Integration testler çalıştırılıyor'
                bat 'mvn clean test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                    echo '📊 Test raporları toplandı'
                }
            }
        }

        // =================================================
        // 4. SISTEMI DOCKER ILE AYAĞA KALDIR
        // =================================================
        stage('Start System (Docker)') {
            steps {
                echo '🐳 Docker servisleri ayağa kaldırılıyor'
                bat '''
                docker-compose down -v
                docker-compose up -d
                '''
            }
        }

        // =================================================
        // 5. SERVISLER HAZIR MI?
        // =================================================
        stage('Wait for Services') {
            steps {
                echo '⏳ Backend hazır mı kontrol ediliyor (PowerShell)'

                powershell '''
                $maxRetry = 30
                $retry = 0

                while ($retry -lt $maxRetry) {
                    try {
                        $response = Invoke-WebRequest -Uri "$env:BACKEND_URL/actuator/health" -UseBasicParsing -TimeoutSec 2
                        if ($response.StatusCode -eq 200) {
                            Write-Host "✅ Backend hazır"
                            exit 0
                        }
                    } catch {
                        Write-Host "⏳ Backend bekleniyor..."
                    }

                    Start-Sleep -Seconds 5
                    $retry++
                }

                Write-Error "❌ Backend zaman aşımına uğradı"
                exit 1
                '''

                echo '⏳ Selenium hazır mı kontrol ediliyor (PowerShell)'

                powershell '''
                $maxRetry = 20
                $retry = 0

                while ($retry -lt $maxRetry) {
                    try {
                        $response = Invoke-WebRequest -Uri "$env:SELENIUM_URL/wd/hub/status" -UseBasicParsing -TimeoutSec 2
                        if ($response.StatusCode -eq 200) {
                            Write-Host "✅ Selenium hazır"
                            exit 0
                        }
                    } catch {
                        Write-Host "⏳ Selenium bekleniyor..."
                    }

                    Start-Sleep -Seconds 3
                    $retry++
                }

                Write-Error "❌ Selenium zaman aşımına uğradı"
                exit 1
                '''
            }
        }


        // =================================================
        // 6. E2E (SELENIUM) TESTLER
        // =================================================
        stage('E2E Tests (Selenium)') {
            steps {
                echo '🌐 Selenium E2E testleri çalıştırılıyor'
                bat 'mvn test -Pe2e'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                    echo '📊 E2E test raporları toplandı'
                }
            }
        }
    }

    post {
        always {
            echo '🧹 Docker ortamı temizleniyor'
            bat 'docker-compose down -v'
        }
        success {
            echo '✅ PIPELINE BAŞARILI – Tüm aşamalar geçti'
        }
        failure {
            echo '❌ PIPELINE BAŞARISIZ – Logları inceleyin'
        }
    }
}
