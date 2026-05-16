pipeline {
    agent any

    // ─────────────────────────────────────────────────────────────
    // 1. ENVIRONMENT VARIABLES
    //    Secrets are loaded from /var/jenkins_home/.env (persistent,
    //    never committed to Git). Place .env on EC2 manually.
    //    DOCKER_REGISTRY is where your images will be pushed.
    //    IMAGE_TAG uses the Git commit SHA so every build is traceable.
    // ─────────────────────────────────────────────────────────────
    environment {
        DOCKER_REGISTRY   = "z4ry"
        IMAGE_TAG         = "${env.GIT_COMMIT?.take(7) ?: 'latest'}"

        // Docker Hub credentials — from Jenkins vault (the only
        // secret not in .env since it's a registry login)
        DOCKER_CREDENTIALS_ID  = "dockerhub-credentials"
    }

    // ─────────────────────────────────────────────────────────────
    // 2. BUILD OPTIONS
    // ─────────────────────────────────────────────────────────────
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))  // Keep last 10 builds
        timeout(time: 60, unit: 'MINUTES')              // Fail-safe: kill if stuck
        disableConcurrentBuilds()                       // No overlapping deployments
        timestamps()                                    // Show timestamps in logs
    }

    // ─────────────────────────────────────────────────────────────
    // 3. TRIGGERS
    //    - Poll SCM every minute (or use GitHub webhook instead)
    //    - Run a full scheduled build every night at midnight
    // ─────────────────────────────────────────────────────────────
    triggers {
        pollSCM('* * * * *')          // Check GitHub every minute for changes
        cron('H 0 * * *')             // Nightly full build at midnight
    }

    // ─────────────────────────────────────────────────────────────
    // STAGES
    // ─────────────────────────────────────────────────────────────
    stages {

        // ── STAGE 1: CHECKOUT ────────────────────────────────────
        stage('Checkout') {
            steps {
                echo "📥 Checking out source code..."
                checkout scm
                sh 'git log --oneline -5'
            }
        }

        // ── STAGE 2: LOAD .env FILE ──────────────────────────────
        // Reads KEY=VALUE pairs from /var/jenkins_home/.env (a
        // persistent location outside the workspace — survives
        // git cleans and workspace wipes). Place it on EC2 manually.
        stage('Load Environment') {
            steps {
                script {
                    def envFile = '/var/jenkins_home/.env'
                    if (fileExists(envFile)) {
                        echo "📄 Loading environment variables from ${envFile}..."
                        def envContent = readFile(envFile).trim()
                        envContent.split('\n').each { line ->
                            line = line.trim()
                            // Skip comments and blank lines
                            if (line && !line.startsWith('#') && line.contains('=')) {
                                def parts = line.split('=', 2)
                                def key = parts[0].trim()
                                def value = parts.length > 1 ? parts[1].trim() : ''
                                if (key && value) {
                                    env."${key}" = value
                                }
                            }
                        }
                        echo "✅ Environment loaded"
                    } else {
                        error "❌ .env not found at ${envFile}. SSH into EC2 and create it."
                    }
                }
            }
        }

        // ── STAGE 3: BUILD & PACKAGE ─────────────────────────────
        // Each service is built with Maven in parallel.
        // mvn clean package already produces JARs, so there's no
        // separate "Package JARs" stage needed.
        stage('Build & Package') {
            parallel {
                stage('eureka-server') {
                    steps { dir('eureka-server')        { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'eureka-server/target/surefire-reports/*.xml' } }
                }
                stage('api-gateway') {
                    steps { dir('api-gateway')          { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'api-gateway/target/surefire-reports/*.xml' } }
                }
                stage('auth-service') {
                    steps { dir('auth-service')         { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'auth-service/target/surefire-reports/*.xml' } }
                }
                stage('template-service') {
                    steps { dir('template-service')     { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'template-service/target/surefire-reports/*.xml' } }
                }
                stage('resume-service') {
                    steps { dir('resume-service')       { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'resume-service/target/surefire-reports/*.xml' } }
                }
                stage('ai-service') {
                    steps { dir('ai-service')           { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'ai-service/target/surefire-reports/*.xml' } }
                }
                stage('export-service') {
                    steps { dir('export-service')       { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'export-service/target/surefire-reports/*.xml' } }
                }
                stage('notification-service') {
                    steps { dir('notification-service')  { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'notification-service/target/surefire-reports/*.xml' } }
                }
                stage('job-match-service') {
                    steps { dir('job-match-service')    { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'job-match-service/target/surefire-reports/*.xml' } }
                }
            }
        }

        // ── STAGE 4: BUILD DOCKER IMAGES ─────────────────────────
        // Each service has its own Dockerfile (multi-stage: Maven build
        // → slim JRE runtime image). We tag each image with both:
        //   1. The Git SHA (e.g., abc1234) — for traceability
        //   2. 'latest' — for easy docker-compose pulls
        stage('Build Docker Images') {
            steps {
                script {
                    def services = [
                        'eureka-server', 'api-gateway', 'auth-service',
                        'template-service', 'resume-service', 'ai-service',
                        'export-service', 'notification-service', 'job-match-service'
                    ]
                    services.each { svc ->
                        sh """
                            docker build \
                                -t ${DOCKER_REGISTRY}/resumade-${svc}:${IMAGE_TAG} \
                                -t ${DOCKER_REGISTRY}/resumade-${svc}:latest \
                                ./${svc}
                        """
                    }
                }
            }
        }

        // ── STAGE 5: PUSH DOCKER IMAGES ──────────────────────────
        // Push to Docker Hub. Only on main branch.
        // Docker Hub credentials come from Jenkins vault (the only
        // secret not in .env since it's a registry login).
        stage('Push Docker Images') {
            when { expression { env.GIT_BRANCH == 'origin/main' } }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: env.DOCKER_CREDENTIALS_ID,
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
                    script {
                        def allImages = [
                            'eureka-server', 'api-gateway', 'auth-service',
                            'template-service', 'resume-service', 'ai-service',
                            'export-service', 'notification-service', 'job-match-service'
                        ]
                        allImages.each { svc ->
                            sh "docker push ${DOCKER_REGISTRY}/resumade-${svc}:${IMAGE_TAG}"
                            sh "docker push ${DOCKER_REGISTRY}/resumade-${svc}:latest"
                        }
                    }
                    sh 'docker logout'
                }
            }
        }

        // ── STAGE 6: DEPLOY ──────────────────────────────────────
        // Pull the freshly pushed images and restart the stack.
        // .env is read from /var/jenkins_home/.env (persistent on EC2).
        stage('Deploy') {
            when { expression { env.GIT_BRANCH == 'origin/main' } }
            steps {
                sh '''
                    if [ ! -f docker-compose.yml ]; then
                        echo "❌ docker-compose.yml not found in repo root!"
                        exit 1
                    fi
                    echo "🚀 Deploying with docker-compose..."
                    docker-compose --env-file /var/jenkins_home/.env pull
                    docker-compose --env-file /var/jenkins_home/.env up -d --force-recreate --remove-orphans
                '''
            }
        }

        // ── STAGE 7: SMOKE TEST ──────────────────────────────────
        // After deployment, verify the key entry points are actually
        // responding. A 60s sleep gives containers time to fully start up.
        stage('Smoke Test') {
            when { expression { env.GIT_BRANCH == 'origin/main' } }
            steps {
                sh 'sleep 60'  // Wait for services to boot
                sh 'curl -f http://localhost:8761/actuator/health || (echo "❌ Eureka is down!" && exit 1)'
                sh 'curl -f http://localhost:9090/actuator/health || (echo "❌ API Gateway is down!" && exit 1)'
                sh 'curl -f http://localhost:80 || (echo "❌ Frontend is down!" && exit 1)'
                echo "✅ Smoke tests passed — deployment successful!"
            }
        }

    } // end stages

    // ─────────────────────────────────────────────────────────────
    // POST ACTIONS: Run regardless of build outcome
    // ─────────────────────────────────────────────────────────────
    post {
        always {
            // Archive JARs as downloadable build artifacts
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
            // Clean up local Docker images to free disk space
            sh 'docker image prune -f || true'
        }
        success {
            echo "🎉 Pipeline SUCCESS — Build #${env.BUILD_NUMBER} deployed."
        }
        failure {
            echo "❌ Pipeline FAILED — Build #${env.BUILD_NUMBER}. Check logs above."
        }
        unstable {
            echo "⚠️ Pipeline UNSTABLE — some tests may have failed."
        }
    }

} // end pipeline
