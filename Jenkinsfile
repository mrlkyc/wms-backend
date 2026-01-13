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

        // =================================================
        // 1. CHECKOUT
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
        // 3. UNIT TESTS
        // =================================================
        stage('Unit Tests') {
            steps {
                echo '🧪 Unit testler çalıştırılıyor'
                bat 'mvn test -Dtest=*ServiceTest'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                    echo '📊 Unit test raporları toplandı'
                }
            }
        }

        // =================================================
        // 4. INTEGRATION TESTS
        // =================================================
        stage('Integration Tests') {
            steps {
                echo '🔗 Integration testler çalıştırılıyor'
                bat 'mvn test -Dtest=*IntegrationTest'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                    echo '📊 Integration test raporları toplandı'
                }
            }
        }
stage('Force Clean Docker') {
    steps {
        echo '🧹 Eski Docker containerları zorla temizleniyor'
        bat '''
        docker rm -f wms-postgres || echo wms-postgres yok
        docker rm -f selenium-chrome || echo selenium-chrome yok
        docker rm -f wms-backend || echo wms-backend yok
        '''
    }
}

        // =================================================
        // 5. SISTEMI DOCKER ILE AYAĞA KALDIR
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
        // 6. SERVISLER HAZIR MI?
        // =================================================
        stage('Wait for Services') {
            steps {
                echo '⏳ Backend hazır mı kontrol ediliyor'

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

                echo '⏳ Selenium hazır mı kontrol ediliyor'

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
        // 7. E2E TESTS (SELENIUM)
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
