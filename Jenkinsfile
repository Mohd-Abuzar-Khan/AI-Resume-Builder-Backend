pipeline {
    agent any

    // ─────────────────────────────────────────────────────────────
    // 1. ENVIRONMENT VARIABLES
    //    Secrets are loaded from /var/jenkins_home/.env (persistent,
    //    never committed to Git). Place .env on the VPS manually.
    //
    //    Required keys in /var/jenkins_home/.env:
    //    ─────────────────────────────────────────
    //    DB_USER=Z4RY
    //    DB_PASSWORD=your_mysql_password
    //    JWT_SECRET=your_jwt_secret
    //    GEMINI_API_KEY=your_gemini_key
    //    JOOBLE_API_KEY=your_jooble_key
    //    RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxx        ← was missing, caused crash
    //    RAZORPAY_KEY_SECRET=your_razorpay_secret   ← was missing, caused crash
    //    GOOGLE_CLIENT_ID=your_google_client_id
    //    GOOGLE_CLIENT_SECRET=your_google_secret
    //    GOOGLE_REDIRECT_URI=https://yourdomain.com/login/oauth2/code/google
    //    MAIL_USER=your@gmail.com
    //    MAIL_PASSWORD=your_app_password
    // ─────────────────────────────────────────────────────────────
    environment {
        DOCKER_REGISTRY        = "z4ry"
        IMAGE_TAG              = "${env.GIT_COMMIT?.take(7) ?: 'latest'}"
        DOCKER_CREDENTIALS_ID  = "dockerhub-credentials"
    }

    // ─────────────────────────────────────────────────────────────
    // 2. BUILD OPTIONS
    // ─────────────────────────────────────────────────────────────
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
        disableConcurrentBuilds()
        timestamps()
    }

    // ─────────────────────────────────────────────────────────────
    // 3. TRIGGERS
    // ─────────────────────────────────────────────────────────────
    triggers {
        pollSCM('* * * * *')   // Check GitHub every minute for changes
        cron('H 0 * * *')      // Nightly full build at midnight
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
        // Reads KEY=VALUE pairs from /var/jenkins_home/.env
        // (persistent on VPS — survives workspace wipes, never in Git).
        stage('Load Environment') {
            steps {
                script {
                    def envFile = '/var/jenkins_home/.env'
                    if (fileExists(envFile)) {
                        echo "📄 Loading environment variables from ${envFile}..."
                        def envContent = readFile(envFile).trim()
                        envContent.split('\n').each { line ->
                            line = line.trim()
                            if (line && !line.startsWith('#') && line.contains('=')) {
                                def parts = line.split('=', 2)
                                def key   = parts[0].trim()
                                def value = parts.length > 1 ? parts[1].trim() : ''
                                if (key && value) {
                                    env."${key}" = value
                                }
                            }
                        }
                        echo "✅ Environment loaded"
                    } else {
                        error "❌ .env not found at ${envFile}. SSH into your VPS and create it. See required keys in this Jenkinsfile above."
                    }
                }
            }
        }

        // ── STAGE 3: VALIDATE ENVIRONMENT ────────────────────────
        // Fail fast — check all required env vars are present before
        // wasting time building JARs and Docker images.
        // This catches missing keys like RAZORPAY_KEY_ID early.
        stage('Validate Environment') {
            steps {
                script {
                    def required = [
                        'DB_USER',
                        'DB_PASSWORD',
                        'JWT_SECRET',
                        'GEMINI_API_KEY',
                        'JOOBLE_API_KEY',
                        'RAZORPAY_KEY_ID',
                        'RAZORPAY_KEY_SECRET',
                        'GOOGLE_CLIENT_ID',
                        'GOOGLE_CLIENT_SECRET',
                        'GOOGLE_REDIRECT_URI',
                        'MAIL_USER',
                        'MAIL_PASSWORD'
                    ]
                    def missing = required.findAll { key -> !env."${key}" }
                    if (missing) {
                        error "❌ Missing required env vars: ${missing.join(', ')}\nAdd them to /var/jenkins_home/.env on your VPS."
                    }
                    echo "✅ All required environment variables are present"
                }
            }
        }

        // ── STAGE 4: BUILD & PACKAGE ─────────────────────────────
        // All 9 services built in parallel with Maven.
        stage('Build & Package') {
            parallel {
                stage('eureka-server') {
                    steps { dir('eureka-server')       { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'eureka-server/target/surefire-reports/*.xml' } }
                }
                stage('api-gateway') {
                    steps { dir('api-gateway')         { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'api-gateway/target/surefire-reports/*.xml' } }
                }
                stage('auth-service') {
                    steps { dir('auth-service')        { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'auth-service/target/surefire-reports/*.xml' } }
                }
                stage('template-service') {
                    steps { dir('template-service')    { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'template-service/target/surefire-reports/*.xml' } }
                }
                stage('resume-service') {
                    steps { dir('resume-service')      { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'resume-service/target/surefire-reports/*.xml' } }
                }
                stage('ai-service') {
                    steps { dir('ai-service')          { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'ai-service/target/surefire-reports/*.xml' } }
                }
                stage('export-service') {
                    steps { dir('export-service')      { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'export-service/target/surefire-reports/*.xml' } }
                }
                stage('notification-service') {
                    steps { dir('notification-service') { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'notification-service/target/surefire-reports/*.xml' } }
                }
                stage('job-match-service') {
                    steps { dir('job-match-service')   { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'job-match-service/target/surefire-reports/*.xml' } }
                }
            }
        }

        // ── STAGE 5: BUILD DOCKER IMAGES ─────────────────────────
        // Tag with Git SHA (traceability) and 'latest' (for compose pulls).
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

        // ── STAGE 6: PUSH DOCKER IMAGES ──────────────────────────
        // Push to Docker Hub. Only runs on main branch.
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
                        def services = [
                            'eureka-server', 'api-gateway', 'auth-service',
                            'template-service', 'resume-service', 'ai-service',
                            'export-service', 'notification-service', 'job-match-service'
                        ]
                        services.each { svc ->
                            sh "docker push ${DOCKER_REGISTRY}/resumade-${svc}:${IMAGE_TAG}"
                            sh "docker push ${DOCKER_REGISTRY}/resumade-${svc}:latest"
                        }
                    }
                    sh 'docker logout'
                }
            }
        }

        // ── STAGE 7: DEPLOY ──────────────────────────────────────
        // Pull freshly pushed images and restart the full stack.
        // Uses 'docker compose' (v2 — docker-compose v1 is deprecated).
        stage('Deploy') {
            when { expression { env.GIT_BRANCH == 'origin/main' } }
            steps {
                sh '''
                    if [ ! -f docker-compose.yml ]; then
                        echo "❌ docker-compose.yml not found in repo root!"
                        exit 1
                    fi
                    echo "🚀 Deploying with docker compose..."
                    docker compose --env-file /var/jenkins_home/.env pull
                    docker compose --env-file /var/jenkins_home/.env up -d --force-recreate --remove-orphans
                '''
            }
        }

        // ── STAGE 8: SMOKE TEST ──────────────────────────────────
        // Verify key services are responding after deployment.
        // 90s wait gives Eureka enough time to fully boot on 4GB RAM.
        stage('Smoke Test') {
            when { expression { env.GIT_BRANCH == 'origin/main' } }
            steps {
                sh 'echo "⏳ Waiting 90s for services to boot..."; sleep 90'
                sh 'curl -sf http://localhost:8761/actuator/health || (echo "❌ Eureka is down!" && exit 1)'
                sh 'curl -sf http://localhost:9090/actuator/health || (echo "❌ API Gateway is down!" && exit 1)'
                sh 'curl -sf http://localhost:9091/actuator/health || (echo "❌ auth-service is down! Check RAZORPAY/JWT env vars." && exit 1)'
                echo "✅ Smoke tests passed — deployment successful!"
            }
        }

    } // end stages

    // ─────────────────────────────────────────────────────────────
    // POST ACTIONS
    // ─────────────────────────────────────────────────────────────
    post {
        always {
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
            sh 'docker image prune -f || true'
        }
        success {
            echo "🎉 Pipeline SUCCESS — Build #${env.BUILD_NUMBER} deployed."
        }
        failure {
            echo "❌ Pipeline FAILED — Build #${env.BUILD_NUMBER}. Check logs above."
            sh 'docker compose logs --tail=50 || true'
        }
        unstable {
            echo "⚠️ Pipeline UNSTABLE — some tests may have failed."
        }
    }

} // end pipeline