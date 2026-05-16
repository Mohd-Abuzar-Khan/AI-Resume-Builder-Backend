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
    //    RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxx
    //    RAZORPAY_KEY_SECRET=your_razorpay_secret
    //    GOOGLE_CLIENT_ID=your_google_client_id
    //    GOOGLE_CLIENT_SECRET=your_google_secret
    //    GOOGLE_REDIRECT_URI=https://yourdomain.com/login/oauth2/code/google
    //    MAIL_USER=your@gmail.com
    //    MAIL_PASSWORD=your_app_password
    // ─────────────────────────────────────────────────────────────
    environment {
        DOCKER_REGISTRY       = "z4ry"
        IMAGE_TAG             = "${env.GIT_COMMIT?.take(7) ?: 'latest'}"
        DOCKER_CREDENTIALS_ID = "dockerhub-credentials"
        DOCKER_BUILDKIT       = "1"
    }

    // ─────────────────────────────────────────────────────────────
    // 2. BUILD OPTIONS
    // ─────────────────────────────────────────────────────────────
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))   // Keep last 5 builds only
        timeout(time: 90, unit: 'MINUTES')              // Multistage builds take longer
        disableConcurrentBuilds()
        timestamps()
    }

    // ─────────────────────────────────────────────────────────────
    // 3. TRIGGERS
    // ─────────────────────────────────────────────────────────────
    triggers {
        pollSCM('* * * * *')
        cron('H 0 * * *')
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
                        error "❌ .env not found at ${envFile}. SSH into your VPS and create it."
                    }
                }
            }
        }

        // ── STAGE 3: VALIDATE ENVIRONMENT ────────────────────────
        stage('Validate Environment') {
            steps {
                script {
                    def required = [
                        'DB_USER', 'DB_PASSWORD', 'JWT_SECRET',
                        'GEMINI_API_KEY', 'JOOBLE_API_KEY',
                        'RAZORPAY_KEY_ID', 'RAZORPAY_KEY_SECRET',
                        'GOOGLE_CLIENT_ID', 'GOOGLE_CLIENT_SECRET', 'GOOGLE_REDIRECT_URI',
                        'MAIL_USER', 'MAIL_PASSWORD'
                    ]
                    def missing = required.findAll { key -> !env."${key}" }
                    if (missing) {
                        error "❌ Missing required env vars: ${missing.join(', ')}\nAdd them to /var/jenkins_home/.env"
                    }
                    echo "✅ All required environment variables are present"
                }
            }
        }

        // ── STAGE 4: BUILD DOCKER IMAGES ─────────────────────────
        // Maven runs INSIDE each Docker multistage build.
        // No Maven needed on Jenkins host — saves disk and complexity.
        // Built sequentially (not parallel) to avoid OOM and disk pressure.
        // Dangling images pruned after each build to keep disk clean.
        stage('Build Docker Images') {
            steps {
                script {
                    def services = [
                        'eureka-server', 'api-gateway', 'auth-service',
                        'template-service', 'resume-service', 'ai-service',
                        'export-service', 'notification-service', 'job-match-service'
                    ]
                    services.each { svc ->
                        echo "🔨 Building ${svc}..."
                        sh """
                            DOCKER_BUILDKIT=0 docker build \
                                -t ${DOCKER_REGISTRY}/resumade-${svc}:${IMAGE_TAG} \
                                -t ${DOCKER_REGISTRY}/resumade-${svc}:latest \
                                ./${svc}
                        """
                        // Prune dangling images after each build to free space immediately
                        sh 'docker image prune -f || true'
                    }
                }
            }
        }

        // ── STAGE 5: PUSH DOCKER IMAGES ──────────────────────────
        // Push to Docker Hub. Only on main branch.
        // Local image deleted after push — Docker Hub has it, no need to keep locally.
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
                            // Remove local copy after push to free disk space
                            sh "docker rmi ${DOCKER_REGISTRY}/resumade-${svc}:${IMAGE_TAG} || true"
                            sh "docker rmi ${DOCKER_REGISTRY}/resumade-${svc}:latest || true"
                        }
                    }
                    sh 'docker logout'
                }
            }
        }

        // ── STAGE 6: DEPLOY ──────────────────────────────────────
        // Pull images from Docker Hub and restart the full stack.
        stage('Deploy') {
            when { expression { env.GIT_BRANCH == 'origin/main' } }
            steps {
                sh '''
                    if [ ! -f docker-compose.yml ]; then
                        echo "❌ docker-compose.yml not found in repo root!"
                        exit 1
                    fi
                    echo "🚀 Deploying with docker compose..."
                    set -a
                    source /var/jenkins_home/.env
                    set +a
                    docker compose pull
                    docker compose up -d --force-recreate --remove-orphans
                '''
            }
        }

        // ── STAGE 7: SMOKE TEST ──────────────────────────────────
        stage('Smoke Test') {
            when { expression { env.GIT_BRANCH == 'origin/main' } }
            steps {
                sh 'echo "⏳ Waiting 90s for services to boot..."; sleep 90'
                sh 'curl -sf http://localhost:8761/actuator/health || (echo "❌ Eureka is down!" && exit 1)'
                sh 'curl -sf http://localhost:9090/actuator/health || (echo "❌ API Gateway is down!" && exit 1)'
                sh 'curl -sf http://localhost:9091/actuator/health || (echo "❌ auth-service is down!" && exit 1)'
                echo "✅ Smoke tests passed — deployment successful!"
            }
        }

    } // end stages

    // ─────────────────────────────────────────────────────────────
    // POST ACTIONS
    // ─────────────────────────────────────────────────────────────
    post {
        always {
            // Prune dangling images after every build regardless of outcome
            sh 'docker image prune -f || true'
        }
        success {
            echo "🎉 Pipeline SUCCESS — Build #${env.BUILD_NUMBER} deployed."
        }
        failure {
            echo "❌ Pipeline FAILED — Build #${env.BUILD_NUMBER}. Check logs above."
            // Fixed: --tail flag not supported on older Docker versions
            sh 'docker compose logs 2>/dev/null | tail -50 || true'
        }
        unstable {
            echo "⚠️ Pipeline UNSTABLE — some tests may have failed."
        }
    }

} // end pipeline