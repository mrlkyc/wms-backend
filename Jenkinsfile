pipeline {
    agent any

    tools {
        maven 'Maven-3.9.6'
        jdk 'JDK17'
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
                echo '⏳ Backend hazır mı kontrol ediliyor'
                bat '''
                FOR /L %%i IN (1,1,30) DO (
                    curl -f %BACKEND_URL%/actuator/health > nul 2>&1
                    IF %ERRORLEVEL% EQU 0 (
                        echo Backend hazir
                        GOTO backend_ok
                    )
                    echo Backend bekleniyor...
                    timeout /t 5 > nul
                )
                echo Backend zaman asimina ugradi
                EXIT /B 1
                :backend_ok
                '''

                echo '⏳ Selenium hazır mı kontrol ediliyor'
                bat '''
                FOR /L %%i IN (1,1,20) DO (
                    curl -f %SELENIUM_URL%/wd/hub/status > nul 2>&1
                    IF %ERRORLEVEL% EQU 0 (
                        echo Selenium hazir
                        GOTO selenium_ok
                    )
                    echo Selenium bekleniyor...
                    timeout /t 3 > nul
                )
                echo Selenium zaman asimina ugradi
                EXIT /B 1
                :selenium_ok
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
