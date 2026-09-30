pipeline {
    agent any

    environment {
        // Hedef Image Registry: GitHub Container Registry (ghcr.io)
        REGISTRY = 'ghcr.io'
        REGISTRY_OWNER = 'aknkya'
        
        // Jenkins Credentials Store'da tanımlayacağınız gizli anahtarın ID'si:
        GHCR_CREDENTIALS_ID = 'github-packages-token'
        
        // Her Jenkins build'inde otomatik artan versiyon numarası (örn: v1.0.1, v1.0.2...)
        IMAGE_TAG = "v1.0.${BUILD_NUMBER}"
    }

    stages {
        // ----------------------------------------------------------------------
        // ADIM 1: Kodu Git'ten Çekme
        // ----------------------------------------------------------------------
        stage('1. Checkout SCM') {
            steps {
                echo "📥 Git deposundan en son kod çekiliyor..."
                checkout scm
            }
        }

        // ----------------------------------------------------------------------
        // ADIM 2: Maven ile Mikroservisleri Derleme
        // ----------------------------------------------------------------------
        stage('2. Maven Build') {
            steps {
                echo "⚙️ Maven ile mikroservisler derleniyor ve JAR üretiliyor..."
                // Linux Jenkins için: ./mvnw, Windows Jenkins için: bat 'mvnw.cmd clean package -DskipTests'
                sh './mvnw clean package -DskipTests'
            }
        }

        // ----------------------------------------------------------------------
        // ADIM 3: Docker İmajlarını Paketleme (3 Mikroservis)
        // ----------------------------------------------------------------------
        stage('3. Docker Build') {
            steps {
                echo "🐳 Docker imajları oluşturuluyor -> Versiyon: ${IMAGE_TAG}"
                sh """
                    docker build -t ${REGISTRY}/${REGISTRY_OWNER}/notification-service:${IMAGE_TAG} ./notification-service
                    docker build -t ${REGISTRY}/${REGISTRY_OWNER}/payment-service:${IMAGE_TAG} ./payment-service
                    docker build -t ${REGISTRY}/${REGISTRY_OWNER}/order-service:${IMAGE_TAG} ./order-service
                """
            }
        }

        // ----------------------------------------------------------------------
        // ADIM 4: İmajları Registry'ye Yükleme (ghcr.io)
        // ----------------------------------------------------------------------
        stage('4. Push Images to Registry') {
            steps {
                echo "🚀 İmajlar ${REGISTRY} adresine yükleniyor..."
                // Şifreyi koda yazmıyoruz; Jenkins'in kasasından (Credentials) çekiyoruz:
                withCredentials([usernamePassword(credentialsId: "${GHCR_CREDENTIALS_ID}", usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
                    sh """
                        echo "\$REG_PASS" | docker login ${REGISTRY} -u "\$REG_USER" --password-stdin
                        docker push ${REGISTRY}/${REGISTRY_OWNER}/notification-service:${IMAGE_TAG}
                        docker push ${REGISTRY}/${REGISTRY_OWNER}/payment-service:${IMAGE_TAG}
                        docker push ${REGISTRY}/${REGISTRY_OWNER}/order-service:${IMAGE_TAG}
                    """
                }
            }
        }
    }

    post {
        success {
            echo "=========================================================================="
            echo "🎉 Pipeline başarıyla tamamlandı!"
            echo "📦 Yüklenen İmajlar:"
            echo "   - ${REGISTRY}/${REGISTRY_OWNER}/order-service:${IMAGE_TAG}"
            echo "   - ${REGISTRY}/${REGISTRY_OWNER}/payment-service:${IMAGE_TAG}"
            echo "   - ${REGISTRY}/${REGISTRY_OWNER}/notification-service:${IMAGE_TAG}"
            echo "=========================================================================="
        }
        failure {
            echo "❌ Pipeline bir adımda hata verdi, lütfen logları inceleyin."
        }
    }
}
