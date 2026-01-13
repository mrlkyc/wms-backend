pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        // Backend portunu kendi projenle aynı yap
        BACKEND_URL = "http://host.docker.internal:9095"
        SELENIUM_URL = "http://host.docker.internal:4444"
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
                sh 'mvn clean package -DskipTests'
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
                sh 'mvn clean test'
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
                sh '''
                    docker-compose down -v || true
                    docker-compose up -d
                '''
            }
        }

        // =================================================
        // 5. SERVISLER HAZIR MI?
        // =================================================
        stage('Wait for Services') {
            steps {
                echo '⏳ Backend ve Selenium hazır mı kontrol ediliyor'
                sh '''
                    set -e

                    echo "➡ Backend health check"
                    for i in {1..30}; do
                        if curl -sf ${BACKEND_URL}/actuator/health > /dev/null; then
                            echo "✅ Backend hazır"
                            break
                        fi
                        echo "⏳ Backend bekleniyor..."
                        sleep 5
                    done

                    echo "➡ Selenium health check"
                    for i in {1..20}; do
                        if curl -sf ${SELENIUM_URL}/wd/hub/status > /dev/null; then
                            echo "✅ Selenium hazır"
                            break
                        fi
                        echo "⏳ Selenium bekleniyor..."
                        sleep 3
                    done
                '''
            }
        }

        // =================================================
        // 6. E2E (SELENIUM) TESTLER
        // =================================================
        stage('E2E Tests (Selenium)') {
            steps {
                echo '🌐 Selenium E2E testleri çalıştırılıyor'
                sh 'mvn test -Pe2e'
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
            sh 'docker-compose down -v || true'
        }
        success {
            echo "✅ PIPELINE BAŞARILI – Tüm aşamalar geçti"
        }
        failure {
            echo "❌ PIPELINE BAŞARISIZ – Logları inceleyin"
        }
    }
}
