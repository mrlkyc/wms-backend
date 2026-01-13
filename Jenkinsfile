pipeline {
    agent any

    tools {
        maven 'maven-3'
        jdk 'jdk-21'
    }

    environment {
        DOCKER_COMPOSE_FILE = 'docker-compose.yml'
        BACKEND_URL = 'http://localhost:8089/actuator/health'
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
                    echo '✅ Build başarılı'
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Unit & Integration Tests') {
            steps {
                echo '🧪 Unit + Integration testler çalıştırılıyor'
                bat 'mvn clean test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                    echo '📊 Test raporları toplandı'
                }
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
                echo '⏳ Backend hazır mı kontrol ediliyor'
                bat '''
                set READY=0
                for /L %%i in (1,1,30) do (
                    curl -s http://localhost:8089/actuator/health >nul
                    if %ERRORLEVEL% EQU 0 (
                        echo Backend hazir
                        set READY=1
                        goto done
                    )
                    echo Backend bekleniyor...
                    timeout /t 5 >nul
                )
                :done
                if %READY% EQU 0 (
                    echo Backend zamaninda ayaga kalkmadi
                    exit /b 1
                )
                '''
            }
        }


        stage('E2E Tests (Selenium)') {
            steps {
                echo '🧪 Selenium E2E testleri çalıştırılıyor'
                bat 'mvn -Dtest=*UiTest test'
            }
        }
    }

    post {
        always {
            echo '🧹 Docker ortamı temizleniyor'
            bat 'docker-compose down -v'
        }

        success {
            echo '🎉 PIPELINE BAŞARILI'
        }

        failure {
            echo '❌ PIPELINE BAŞARISIZ – Logları inceleyin'
        }
    }
}
