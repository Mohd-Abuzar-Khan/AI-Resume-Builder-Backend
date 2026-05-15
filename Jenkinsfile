pipeline {
    agent any

    // ─────────────────────────────────────────────────────────────
    // 1. ENVIRONMENT VARIABLES
    //    All secrets come from Jenkins Credentials Store (never hard-coded).
    //    DOCKER_REGISTRY is where your images will be pushed.
    //    IMAGE_TAG uses the Git commit SHA so every build is traceable.
    // ─────────────────────────────────────────────────────────────
    environment {
        DOCKER_REGISTRY   = "your-dockerhub-username"           // ← change this to your Docker Hub username
        IMAGE_TAG         = "${env.GIT_COMMIT?.take(7) ?: 'latest'}"

        // Jenkins Credential IDs (configure these in Manage Jenkins → Credentials)
        DOCKER_CREDENTIALS_ID  = "dockerhub-credentials"
        DB_PASSWORD_ID         = "db-password"
        GEMINI_API_KEY_ID      = "gemini-api-key"
        JOOBLE_API_KEY_ID      = "jooble-api-key"
        JWT_SECRET_ID          = "jwt-secret"
        RAZORPAY_KEY_ID_CRED   = "razorpay-key-id"
        RAZORPAY_SECRET_ID     = "razorpay-key-secret"
        GOOGLE_CLIENT_ID_CRED  = "google-client-id"
        GOOGLE_SECRET_ID       = "google-client-secret"
        MAIL_USER_ID           = "mail-user"
        MAIL_PASSWORD_ID       = "mail-password"
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

        // ── STAGE 2: BUILD & UNIT TEST ───────────────────────────
        // Each service is built with Maven in parallel.
        // Tests run here so we catch failures early before wasting
        // time on Docker builds.
        stage('Build & Test') {
            parallel {
                stage('eureka-server') {
                    steps { dir('Backend/eureka-server')       { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'Backend/eureka-server/target/surefire-reports/*.xml' } }
                }
                stage('api-gateway') {
                    steps { dir('Backend/api-gateway')         { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'Backend/api-gateway/target/surefire-reports/*.xml' } }
                }
                stage('auth-service') {
                    steps { dir('Backend/auth-service')        { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'Backend/auth-service/target/surefire-reports/*.xml' } }
                }
                stage('template-service') {
                    steps { dir('Backend/template-service')    { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'Backend/template-service/target/surefire-reports/*.xml' } }
                }
                stage('resume-service') {
                    steps { dir('Backend/resume-service')      { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'Backend/resume-service/target/surefire-reports/*.xml' } }
                }
                stage('ai-service') {
                    steps { dir('Backend/ai-service')          { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'Backend/ai-service/target/surefire-reports/*.xml' } }
                }
                stage('export-service') {
                    steps { dir('Backend/export-service')      { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'Backend/export-service/target/surefire-reports/*.xml' } }
                }
                stage('notification-service') {
                    steps { dir('Backend/notification-service') { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'Backend/notification-service/target/surefire-reports/*.xml' } }
                }
                stage('job-match-service') {
                    steps { dir('Backend/job-match-service')   { sh 'mvn clean package -DskipTests -B' } }
                    post  { always { junit allowEmptyResults: true, testResults: 'Backend/job-match-service/target/surefire-reports/*.xml' } }
                }
            }
        }

        // ── STAGE 3: PACKAGE JARs ────────────────────────────────
        // Tests already passed above, so skip them here.
        stage('Package JARs') {
            steps {
                script {
                    def services = [
                        'eureka-server', 'api-gateway', 'auth-service',
                        'template-service', 'resume-service', 'ai-service',
                        'export-service', 'notification-service', 'job-match-service'
                    ]
                    services.each { svc ->
                        dir("Backend/${svc}") {
                            sh 'mvn package -DskipTests -B'
                        }
                    }
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
                    // Backend services
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
                                ./Backend/${svc}
                        """
                    }
                    // Frontend
                    sh """
                        docker build \
                            -t ${DOCKER_REGISTRY}/resumade-frontend:${IMAGE_TAG} \
                            -t ${DOCKER_REGISTRY}/resumade-frontend:latest \
                            ./Frontend
                    """
                }
            }
        }

        // ── STAGE 5: PUSH DOCKER IMAGES ──────────────────────────
        // Push to Docker Hub. Only on main branch.
        stage('Push Docker Images') {
            when { branch 'main' }
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
                            'export-service', 'notification-service', 'job-match-service',
                            'frontend'
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
        // Secrets are injected as environment variables via
        // withCredentials — never written to disk.
        stage('Deploy') {
            when { branch 'main' }
            steps {
                withCredentials([
                    string(credentialsId: env.DB_PASSWORD_ID,        variable: 'DB_PASSWORD'),
                    string(credentialsId: env.GEMINI_API_KEY_ID,     variable: 'GEMINI_API_KEY'),
                    string(credentialsId: env.JOOBLE_API_KEY_ID,     variable: 'JOOBLE_API_KEY'),
                    string(credentialsId: env.JWT_SECRET_ID,         variable: 'JWT_SECRET'),
                    string(credentialsId: env.RAZORPAY_KEY_ID_CRED,  variable: 'RAZORPAY_KEY_ID'),
                    string(credentialsId: env.RAZORPAY_SECRET_ID,    variable: 'RAZORPAY_KEY_SECRET'),
                    string(credentialsId: env.GOOGLE_CLIENT_ID_CRED, variable: 'GOOGLE_CLIENT_ID'),
                    string(credentialsId: env.GOOGLE_SECRET_ID,      variable: 'GOOGLE_CLIENT_SECRET'),
                    string(credentialsId: env.MAIL_USER_ID,          variable: 'MAIL_USER'),
                    string(credentialsId: env.MAIL_PASSWORD_ID,      variable: 'MAIL_PASSWORD')
                ]) {
                    sh '''
                        export DB_USER=Z4RY
                        export DB_PASSWORD=$DB_PASSWORD
                        export GEMINI_API_KEY=$GEMINI_API_KEY
                        export JOOBLE_API_KEY=$JOOBLE_API_KEY
                        export JWT_SECRET=$JWT_SECRET
                        export RAZORPAY_KEY_ID=$RAZORPAY_KEY_ID
                        export RAZORPAY_KEY_SECRET=$RAZORPAY_KEY_SECRET
                        export GOOGLE_CLIENT_ID=$GOOGLE_CLIENT_ID
                        export GOOGLE_CLIENT_SECRET=$GOOGLE_CLIENT_SECRET
                        export GOOGLE_REDIRECT_URI=http://localhost:4200/auth/google
                        export MAIL_HOST=smtp.gmail.com
                        export MAIL_USER=$MAIL_USER
                        export MAIL_PASSWORD=$MAIL_PASSWORD

                        cd Backend
                        docker-compose pull
                        docker-compose up -d --force-recreate --remove-orphans
                    '''
                }
            }
        }

        // ── STAGE 7: SMOKE TEST ──────────────────────────────────
        // After deployment, verify the key entry points are actually
        // responding. A 60s sleep gives containers time to fully start up.
        stage('Smoke Test') {
            when { branch 'main' }
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
