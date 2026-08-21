pipeline {
    agent any

    tools {
        jdk 'temurin-17'
        nodejs 'node-22'
    }

    environment {
        BACKEND_IMAGE = 'payrecon-backend'
        FRONTEND_IMAGE = 'payrecon-frontend'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Unit Test') {
            parallel {
                stage('Backend') {
                    steps {
                        dir('backend') {
                            sh './mvnw -B clean verify'
                        }
                    }
                    post {
                        always {
                            junit 'backend/target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Frontend') {
                    steps {
                        dir('frontend') {
                            sh 'npm ci'
                            sh 'npx ng test --watch=false'
                            sh 'npx ng build --configuration production'
                        }
                    }
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} ./backend"
                sh "docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} ./frontend"
            }
        }

        stage('Deploy') {
            steps {
                // Simulated for the portfolio demo: a real pipeline would push
                // the tagged images to a registry here and trigger a rollout
                // (e.g. `kubectl set image`, or `docker compose pull && up -d`
                // against a remote host) rather than just logging.
                echo "Would deploy ${BACKEND_IMAGE}:${IMAGE_TAG} and ${FRONTEND_IMAGE}:${IMAGE_TAG}"
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
