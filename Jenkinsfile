pipeline {
    agent {
        docker {
            image 'maven:3.9.6-eclipse-temurin-17'
            args '-v /root/.m2:/root/.m2'
        }
    }

    environment {
        JWT_SECRET      = credentials('reg-tracker-jwt-secret')
        DOCKER_REGISTRY = 'registry.example.com'
        IMAGE_NAME      = 'regulatory-submission-tracker'
        MAVEN_OPTS      = '-Dmaven.repo.local=/root/.m2/repository'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                sh '''
                    mvn verify -B \
                        -Dspring.profiles.active=test \
                        -DJWT_SECRET="${JWT_SECRET}" \
                        -DAWS_REGION=us-east-1 \
                        -DS3_BUCKET=jenkins-test-bucket \
                        -DMAIL_HOST=
                '''
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Build JAR') {
            steps {
                sh 'mvn package -DskipTests -B'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Docker Build & Push') {
            when {
                branch 'main'
            }
            agent { label 'docker' }
            steps {
                script {
                    def image = docker.build("${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.BUILD_NUMBER}")
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'docker-registry-credentials') {
                        image.push()
                        image.push('latest')
                    }
                }
            }
        }

        stage('Deploy to Dev') {
            when {
                branch 'main'
            }
            steps {
                sh """
                    kubectl set image deployment/reg-tracker \
                        reg-tracker=${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.BUILD_NUMBER} \
                        --namespace=dev
                    kubectl rollout status deployment/reg-tracker --namespace=dev --timeout=120s
                """
            }
        }
    }

    post {
        failure {
            mail to: 'team@pharma.example.com',
                 subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                 body: "Build failed: ${env.BUILD_URL}"
        }
    }
}
