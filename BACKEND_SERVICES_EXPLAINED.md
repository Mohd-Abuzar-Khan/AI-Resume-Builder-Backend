# Backend Services Explanation Guide

This guide explains each backend service as if you're learning about them for the first time. Think of this as a friendly walkthrough of what each service does and how they all work together.

## The Big Picture: Microservices Architecture

Imagine you're building a resume application. Instead of putting all the code in one big monolith, we split it into smaller, specialized services. Each service has one job, does it well, and talks to the others when needed.

Here's our system:

```
User (Frontend) 
    ↓
    ↓ (HTTP requests)
    ↓
API Gateway (Port 9090)
    ↓ (routes to appropriate service)
    ├→ Eureka Server (keeps track of where services are)
    ├→ Auth Service (handles login/signup)
    ├→ Template Service (provides resume templates)
    ├→ Resume Service (stores user resumes)
    ├→ AI Service (provides AI features)
    ├→ Export Service (exports resumes to PDF)
    ├→ Notification Service (sends emails)
    └→ Job Match Service (finds matching jobs)
```

---

## 1. Eureka Server (Port 8761)

**What it does:** Acts like a phonebook for your microservices.

### The Problem It Solves
When you have many services running, they need to know where to find each other. In development, you might have:
- Auth Service on port 9091
- Resume Service on port 9093
- Template Service on port 9092

But what if a service goes down and restarts on a different port? What if you're running multiple instances for load balancing?

### The Solution
Eureka keeps a live registry. When a service starts up, it says "Hello! I'm the Auth Service and I'm at http://auth-service:9091". Other services can ask Eureka "Where's the Auth Service?" and get the answer instantly.

### File Reference
[Backend/eureka-server/src/main/resources/application.yml](Backend/eureka-server/src/main/resources/application.yml)

```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false  # Eureka doesn't register itself
    fetch-registry: false         # It doesn't need to fetch other services
  server:
    enable-self-preservation: false  # Always remove dead services
```

### In Practice
- **Resume Service starts**: "I'm here at http://resume-service:9093" → registers with Eureka
- **AI Service needs to call Resume Service**: Asks Eureka "Where's resume-service?" → Gets the URL
- **Resume Service goes down**: Eureka marks it as unhealthy and removes it from the registry

---

## 2. API Gateway (Port 9090)

**What it does:** Acts as the front door of your application.

### The Problem It Solves
Imagine the frontend has to know about every single service:
- POST /auth-service/register
- POST /resume-service/create
- POST /ai-service/analyze
- etc.

This is messy. The frontend shouldn't need to know the internal structure.

### The Solution
The API Gateway is a single entry point. All requests go through it. The gateway:
1. **Routes requests** to the right service
2. **Validates JWTs** (checks if user is logged in)
3. **Adds user info** to headers so services know who's making the request
4. **Handles CORS** (allows frontend to make requests)

### File Reference
[Backend/api-gateway/src/main/resources/application.yml](Backend/api-gateway/src/main/resources/application.yml)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service                    # Load-balanced route to auth-service
          predicates:
            - Path=/api/v1/auth/**                 # Route all /api/v1/auth/* requests here
        
        - id: resume-service
          uri: lb://resume-service
          predicates:
            - Path=/api/v1/resumes/**              # Route resume endpoints here
        
        - id: ai-service
          uri: lb://ai-service
          predicates:
            - Path=/api/v1/ai/**                   # Route AI endpoints here
```

### In Practice
**Frontend requests:**
```
POST http://localhost:9090/api/v1/auth/login
  → Gateway sees path starts with /api/v1/auth
  → Routes to Auth Service (9091)

POST http://localhost:9090/api/v1/resumes
  → Gateway sees path starts with /api/v1/resumes
  → Routes to Resume Service (9093)
```

### JWT Validation Flow
```
Frontend: "Here's my JWT token"
  ↓
Gateway Filter: Extracts token, validates it
  ↓
Adds headers to request:
  X-User-Id: 123
  X-User-Role: USER
  X-User-Plan: FREE
  ↓
Routes to appropriate service
  ↓
Service: "Thanks! I know who this user is"
```

---

## 3. Auth Service (Port 9091)

**What it does:** Handles user authentication, registration, and account management.

### What Problems Does It Solve?
1. How do users create accounts?
2. How do users log in?
3. How do we generate secure tokens?
4. How do we manage passwords?
5. How do users upgrade their plans?

### File References
Main files:
- [Backend/auth-service/src/main/java/com/resumade/auth/service/AuthServiceImpl.java](Backend/auth-service/src/main/java/com/resumade/auth/service/AuthServiceImpl.java)
- [Backend/auth-service/src/main/java/com/resumade/auth/security/JwtService.java](Backend/auth-service/src/main/java/com/resumade/auth/security/JwtService.java)
- [Backend/auth-service/src/main/java/com/resumade/auth/controller/AuthController.java](Backend/auth-service/src/main/java/com/resumade/auth/controller/AuthController.java)

### Key Workflows

#### Registration
```
User submits: fullName, email, password
  ↓
Check: Is email already taken?
  ↓
YES → Throw error: "Email already exists"
NO → Continue
  ↓
Hash password using BCrypt
  ↓
Create new User in database
  ↓
Generate JWT token containing:
  - email
  - userId
  - role (USER or ADMIN)
  - subscription plan (FREE or PREMIUM)
  - credits
  ↓
Store token in Redis (for logout tracking)
  ↓
Send welcome email via RabbitMQ
  ↓
Return: { token, userId, email, plan, credits }
```

#### Login
```
User submits: email, password
  ↓
Retrieve user from database
  ↓
Compare submitted password with stored BCrypt hash
  ↓
Password incorrect? → Return 401 Unauthorized
  ↓
Password correct? Generate JWT token
  ↓
Store token in Redis
  ↓
Return: { token, userId, email, plan, credits }
```

#### Google OAuth Login
```
User clicks "Login with Google"
  ↓
Frontend gets Google ID token
  ↓
Frontend sends token to: POST /api/v1/auth/google
  ↓
Auth Service verifies token with Google's servers
  ↓
Token invalid? → Return 401 error
  ↓
Token valid? Extract: email, name, picture
  ↓
Look up user by email in database
  ↓
User exists? → Update provider to GOOGLE
  ↓
User doesn't exist? → Create new user
  ↓
Generate JWT and store in Redis
  ↓
Return: { token, userId, email, plan, credits }
```

#### Logout
```
User clicks logout
  ↓
Frontend sends: DELETE /api/v1/auth/logout with JWT
  ↓
Auth Service extracts email from JWT
  ↓
Deletes "JWT_TOKEN:{email}" from Redis
  ↓
Next time user tries to use old token:
  - JWT validation passes (signature is correct)
  - But Redis lookup fails (token was deleted)
  - Request denied: 401 Unauthorized
```

### Key Entities

**User Table:**
- userId, email, passwordHash
- role (USER/ADMIN)
- provider (LOCAL/GOOGLE/LINKEDIN)
- subscriptionPlan (FREE/PREMIUM)
- credits (used for AI features)
- isActive (soft delete flag)

**PaymentRecord Table:**
- Tracks Razorpay payment attempts
- Stores orderId, paymentId, amount, status
- Links to userId

### Key Features
1. **JWT Tokens**: Secure, stateless tokens that contain user info
2. **Redis Token Tracking**: Stores active tokens; deletion forces logout
3. **Plan Enforcement**: Tracks FREE vs PREMIUM subscriptions
4. **Credit System**: Users have credits for AI features (currently disabled)
5. **Password Reset**: Users can request password reset tokens via email
6. **Admin Capabilities**: Admins can manage users, view stats, etc.

---

## 4. Template Service (Port 9092)

**What it does:** Manages resume templates - the visual blueprints for resumes.

### What Is a Template?
Think of templates like resume designs. A template defines:
- HTML layout structure
- CSS styling (colors, fonts, spacing)
- Which sections appear (experience, education, etc.)
- How each section is styled

Example:
- **Professional Classic**: Formal, traditional resume design
- **Modern Minimal**: Clean, minimalist design
- **Two-Column Executive**: Two-column layout with sidebar

### File References
- [Backend/template-service/src/main/java/com/resumade/template/service/TemplateServiceImpl.java](Backend/template-service/src/main/java/com/resumade/template/service/TemplateServiceImpl.java)
- [Backend/template-service/src/main/java/com/resumade/template/controller/TemplateController.java](Backend/template-service/src/main/java/com/resumade/template/controller/TemplateController.java)
- [Backend/template-service/src/main/java/com/resumade/template/entity/Template.java](Backend/template-service/src/main/java/com/resumade/template/entity/Template.java)

### Workflows

#### Browsing Templates
```
Frontend: GET /api/v1/templates/free
  ↓
Template Service queries database: 
  "Get all templates where isPremium=false AND isActive=true"
  ↓
Returns list of 50+ free templates
  ↓
Frontend displays them to user
```

#### Template Filtering
```
Frontend: GET /api/v1/templates/category/PROFESSIONAL
  ↓
Template Service filters by category
  ↓
Returns professional templates only

Frontend: GET /api/v1/templates/popular
  ↓
Template Service orders by usageCount DESC
  ↓
Returns most-used templates first
```

#### Incrementing Usage
```
User selects a template to create resume
  ↓
Frontend: POST /api/v1/templates/{id}/usage
  ↓
Template Service: template.usageCount++
  ↓
Saves back to database
  ↓
Now this template ranks higher in "popular"
```

### Template Structure (In Database)
```
Template {
  templateId: 1
  name: "Professional Classic"
  description: "Timeless resume design"
  category: "PROFESSIONAL"
  isPremium: false
  isActive: true
  usageCount: 1250
  
  htmlLayout: "<div class='resume'>{{content}}</div>"
  cssStyles: ".resume { font-family: Arial; ... }"
  
  layoutConfig: {
    "fonts": {
      "heading": "Playfair Display",
      "body": "Source Sans Pro"
    },
    "colors": {
      "accent": "#1F3A6E",
      "text": "#333333"
    },
    "sections": [
      { "type": "SUMMARY", "enabled": true },
      { "type": "EXPERIENCE", "enabled": true },
      { "type": "EDUCATION", "enabled": true },
      { "type": "SKILLS", "enabled": true }
    ]
  }
}
```

### Key Concepts
1. **Free vs Premium**: Some templates are locked behind paywall
2. **Categories**: Organized by type (Professional, Creative, Minimalist, etc.)
3. **Usage Tracking**: Popular templates float to the top
4. **Admin Customization**: Admins can create, update, deactivate templates
5. **Seeding**: On startup, 3 default templates are created if database is empty

---

## 5. Resume Service (Port 9093)

**What it does:** Manages user resumes - the actual content that users create and edit.

### What Is a Resume?
A resume is:
- Owned by a user
- Uses a template for styling
- Contains multiple sections (experience, education, skills, etc.)
- Can be private (draft) or public (gallery)
- Can be duplicated to create variations

### File References
- [Backend/resume-service/src/main/java/com/resumade/resume/service/ResumeServiceImpl.java](Backend/resume-service/src/main/java/com/resumade/resume/service/ResumeServiceImpl.java)
- [Backend/resume-service/src/main/java/com/resumade/resume/entity/Resume.java](Backend/resume-service/src/main/java/com/resumade/resume/entity/Resume.java)
- [Backend/resume-service/src/main/java/com/resumade/resume/entity/ResumeSection.java](Backend/resume-service/src/main/java/com/resumade/resume/entity/ResumeSection.java)

### Workflows

#### Creating a Resume
```
User: "I want to create a new resume"
Frontend: POST /api/v1/resumes
  Body: {
    title: "Software Engineer Resume",
    targetJobTitle: "Senior Developer",
    templateId: 5
  }
  ↓
Resume Service receives request (knows userId from header)
  ↓
Check plan quota:
  - If FREE plan: Can only have 3 resumes
  - Count existing resumes
  - If already have 3: Return error "Upgrade to premium"
  ↓
Create new Resume:
  - resumeId: auto-generated
  - userId: from header
  - title: from request
  - templateId: from request
  - status: DRAFT (not complete)
  - isPublic: false (private by default)
  ↓
Save to database
  ↓
Return: Resume object with resumeId
```

#### Adding Sections to Resume
```
User: "I want to add my work experience"
Frontend: POST /api/v1/sections/resume/{resumeId}
  Body: {
    sectionType: "EXPERIENCE",
    title: "Work Experience",
    content: {
      jobs: [
        {
          company: "Google",
          title: "Software Engineer",
          dates: "2020-2023",
          bullets: ["Did important things", "Shipped features"]
        }
      ]
    }
  }
  ↓
Section Service receives request
  ↓
Verify ownership: Does userId own this resume?
  ↓
Create new ResumeSection:
  - sectionId: auto-generated
  - resumeId: from URL
  - sectionType: EXPERIENCE
  - title: "Work Experience"
  - content: JSON string (as provided)
  - displayOrder: auto-assigned
  - isVisible: true (shown by default)
  ↓
Save to database
  ↓
Return: ResumeSection object
```

#### Viewing Your Resumes
```
User: "Show me all my resumes"
Frontend: GET /api/v1/resumes
  Headers: X-User-Id: 123
  ↓
Resume Service queries database:
  "Get all resumes where userId = 123, ordered by updatedAt DESC"
  ↓
Returns list with latest resumes first
```

#### Publishing a Resume to Gallery
```
User: "Let other people see my resume"
Frontend: PUT /api/v1/resumes/{resumeId}/publish
  Body: { isPublic: true, ownerName: "John Doe", ownerAvatar: "..." }
  ↓
Resume Service:
  - Sets isPublic = true
  - Stores ownerName and ownerAvatar
  - Updates status to PUBLISHED
  ↓
Now resume appears in:
  - GET /api/v1/resumes/public (public gallery)
  - Visible to all users
  - View count increments when others view it
```

#### Duplicating a Resume
```
User: "I want a copy of my resume to modify for another job"
Frontend: POST /api/v1/resumes/{resumeId}/duplicate
  ↓
Resume Service:
  - Fetch original resume
  - Create new resume with same templateId
  - Append "(Copy)" to title
  - Copy all sections from original
  - Assign new displayOrder values
  - Set status to DRAFT
  - Set isPublic to false
  ↓
Return: New resume object
```

### Resume Structure
```
Resume {
  resumeId: 1
  userId: 42
  title: "Software Engineer Resume"
  targetJobTitle: "Senior Developer"
  templateId: 5          # Links to Template table
  status: DRAFT          # Or COMPLETE, PUBLISHED
  
  atsScore: 0            # Updated by AI Service
  
  isPublic: false        # Private by default
  viewCount: 0           # Incremented when public resume viewed
  
  language: "en"
  ownerName: null        # Filled when published
  ownerAvatar: null
  
  sections: [            # One-to-many relationship
    {
      sectionId: 1,
      sectionType: "EXPERIENCE",
      title: "Work Experience",
      content: { ... JSON ... },
      displayOrder: 1,
      isVisible: true,
      aiGenerated: false
    },
    {
      sectionId: 2,
      sectionType: "EDUCATION",
      title: "Education",
      content: { ... JSON ... },
      displayOrder: 2,
      isVisible: true,
      aiGenerated: false
    }
  ]
}
```

### Plan Enforcement
```
FREE Plan: Maximum 3 resumes
  - Try to create 4th resume → Error
  - "Upgrade to premium for unlimited resumes"

PREMIUM Plan: Unlimited resumes
  - Can create as many as you want
```

---

## 6. AI Service (Port 9094)

**What it does:** Provides AI-powered features using Google's Gemini API.

### What AI Features Does It Offer?
1. **ATS Score**: Analyzes how ATS-friendly your resume is
2. **Tailor Resume**: Rewrites resume to match a job description
3. **Generate Summary**: Creates professional summary based on role
4. **Generate Bullet Points**: Creates achievement bullets
5. **Improve Section**: Rewrites resume sections in different tones
6. **Generate Cover Letter**: Creates personalized cover letter

### File References
- [Backend/ai-service/src/main/java/com/resumade/ai/service/AiServiceImpl.java](Backend/ai-service/src/main/java/com/resumade/ai/service/AiServiceImpl.java)
- [Backend/ai-service/src/main/java/com/resumade/ai/controller/AiController.java](Backend/ai-service/src/main/java/com/resumade/ai/controller/AiController.java)

### How ATS Score Works
```
User: "Check how ATS-friendly my resume is"
Frontend: POST /api/v1/ai/ats-check
  Body: {
    resumeId: 1,
    resumeContent: "Full resume text",
    jobDescription: "Full job description"
  }
  ↓
AI Service:
  1. Save request to database (for history tracking)
  2. Create detailed prompt for Gemini:
     "Analyze this resume against this job description.
      Score out of 100 on:
      - Keyword match (35 points)
      - Experience relevance (25 points)
      - Quantified achievements (20 points)
      - Format & readability (10 points)
      - Summary alignment (10 points)"
  
  3. Call Gemini API with prompt
  4. Parse response JSON
  5. Extract structured report:
     {
       "score": 78,
       "breakdown": {
         "keywordMatch": { "score": 28, "maxScore": 35 },
         "experienceRelevance": { "score": 22, "maxScore": 25 },
         ...
       },
       "keywordsFound": ["Python", "Java", ...],
       "keywordsMissing": ["Docker", "Kubernetes"],
       "suggestions": [
         { "priority": "HIGH", "action": "Add Docker experience" },
         ...
       ]
     }
  ↓
Return: Detailed ATS report
```

### How Tailor Resume Works
```
User: "Rewrite my resume for this specific job"
Frontend: POST /api/v1/ai/tailor
  Body: {
    resumeId: 1,
    resumeContent: "Full resume text",
    jobDescription: "Full job description"
  }
  ↓
AI Service:
  1. Save request to database
  2. Create prompt:
     "Given this resume and job description,
      rewrite the resume to better match the job.
      Keep all real experience - don't invent anything.
      Prioritize keywords from the job description naturally.
      Return JSON with:
      - matchScore (0-100)
      - rewritten summary
      - rewritten skills
      - rewritten experience bullets"
  
  3. Call Gemini API
  4. Parse response
  5. Return tailored resume structure
```

### Key Concepts
1. **Prompt Engineering**: Carefully crafted prompts to get structured JSON responses from Gemini
2. **Request Tracking**: Every AI request is logged in database for history
3. **Failover**: If main Gemini model fails, retry with fallback model
4. **Temperature Setting**: Different temperatures for different tasks:
   - ATS (0.2): Deterministic, consistent scoring
   - Tailor (0.2): Deterministic, consistent tailoring
   - Generate (0.7): Creative, varied output
5. **Error Handling**: If Gemini fails, returns a fallback error report

### Gemini Models Used
- Primary: `gemini-2.5-flash` (fast, efficient)
- Fallback: `gemini-2.5-flash` (same model, for retry logic)

---

## 7. Export Service (Port 9095)

**What it does:** Converts resumes to downloadable formats (PDF, JSON) and stores them.

### What Can You Export?
1. **PDF**: Download resume as PDF file
2. **JSON**: Export structured resume data

### How It Works

```
User: "Export my resume as PDF"
Frontend: POST /api/v1/exports
  Body: {
    resumeId: 1,
    format: "PDF"
  }
  ↓
Export Service:
  1. Create export job
  2. Queue it on RabbitMQ (asynchronous processing)
  3. Return: "Your export is being processed"
  ↓
RabbitMQ processes the job:
  1. Fetch full resume content
  2. Apply template styling (HTML + CSS)
  3. Convert HTML to PDF
  4. Upload to S3 bucket (mocked currently)
  5. Store export record with download URL
  ↓
User receives notification:
  "Your resume is ready to download"
  ↓
User downloads PDF from S3 link
```

### File References
- [Backend/export-service/src/main/resources/application.yml](Backend/export-service/src/main/resources/application.yml)

### Key Features
1. **Asynchronous Processing**: Uses RabbitMQ queue so exports don't block
2. **S3 Storage**: Stores exported files (currently mocked)
3. **Notification Integration**: Notifies user when export is ready
4. **Format Flexibility**: Can export to multiple formats

---

## 8. Notification Service (Port 9096)

**What it does:** Sends emails and system notifications to users.

### What Notifications Are Sent?
1. **Welcome Email**: When user registers
2. **Plan Upgrade Notification**: When user upgrades to premium
3. **Password Reset Email**: When user requests password reset
4. **Export Ready Email**: When resume export is complete
5. **Job Match Email**: When matching jobs are found

### How It Works

```
Another service wants to send a notification:
  ↓
Publishes message to RabbitMQ:
  {
    "userId": 42,
    "recipientEmail": "user@example.com",
    "type": "WELCOME",
    "title": "Welcome to Resumade!",
    "message": "Thanks for signing up...",
    "channel": "EMAIL"  // Or "SMS", "PUSH", "BOTH"
  }
  ↓
RabbitMQ queues the message
  ↓
Notification Service receives it:
  1. Render email template
  2. Connect to SMTP server (Gmail)
  3. Send email
  4. Log notification in database
  ↓
User receives email
```

### Email Templates
Located in: `src/main/resources/templates/`

Examples:
- `welcome-email.html`: Welcome template
- `upgrade-email.html`: Plan upgrade template
- `reset-password-email.html`: Password reset template

### File References
- [Backend/notification-service/src/main/resources/application.yml](Backend/notification-service/src/main/resources/application.yml)

### Key Features
1. **Multi-Channel**: Can send via email, SMS, push, or combination
2. **Templated**: HTML email templates for professional formatting
3. **Asynchronous**: Doesn't block the calling service
4. **Reliable**: Uses RabbitMQ to ensure delivery

---

## 9. Job Match Service (Port 9097)

**What it does:** Finds real job listings that match user resumes.

### How It Works

```
User: "Find jobs that match my resume"
Frontend: GET /api/v1/job-matches?resumeId=1
  ↓
Job Match Service:
  1. Fetch user's resume
  2. Extract skills and experience
  3. Call Jooble API (job listing aggregator):
     "Find jobs for: Senior Software Engineer, Python, AWS"
  ↓
Jooble returns list of 50+ job listings
  ↓
AI Analysis: For each job:
  - Call Gemini to analyze match score
  - "How well does this resume match this job?"
  - Returns: matchScore (0-100)
  ↓
Sort by match score descending
  ↓
Return: Top 20 matching jobs with scores
  ↓
User sees:
  - Job title
  - Company
  - Description
  - Match score %
  - Link to apply
```

### Integration Points
1. **Jooble API**: Fetches real job listings
2. **Gemini API**: Analyzes match quality
3. **Resume Service**: Fetches user's resume content

### File References
- [Backend/job-match-service/src/main/resources/application.yml](Backend/job-match-service/src/main/resources/application.yml)

---

## How Services Talk to Each Other

### Service-to-Service Communication

#### Direct HTTP Calls (with Eureka)
```
AI Service needs resume content:
  ↓
AI Service: "Hey Eureka, where's resume-service?"
Eureka: "It's at http://resume-service:9093"
  ↓
AI Service: GET http://resume-service:9093/api/v1/resumes/1
Resume Service: Returns resume JSON
```

#### Message Queue (RabbitMQ)
```
Auth Service: "I need to send a welcome email"
  ↓
Publishes to RabbitMQ:
  Queue: "notification.queue"
  Message: { userId: 42, type: "WELCOME", ... }
  ↓
Notification Service listening on this queue
  ↓
Receives message, sends email
  ↓
Auth Service doesn't wait - continues processing
```

### Database Isolation

Each service has its own database:
- `resumade_auth`: Auth Service
- `resumade_templates`: Template Service
- `resumade_resumes`: Resume Service
- `resumade_ai`: AI Service
- `resumade_export`: Export Service
- `resumade_notifications`: Notification Service
- `resumade_job_match`: Job Match Service

**Why?** Each service owns its data. Other services can't directly access another service's database. If they need data, they call the service via HTTP.

---

## The Flow: Complete User Journey

### User Registers & Creates Resume

```
1. USER REGISTERS
   Frontend: POST http://localhost:9090/api/v1/auth/register
   ↓
   API Gateway routes to Auth Service
   ↓
   Auth Service:
     - Validates email doesn't exist
     - Hashes password
     - Creates user in database
     - Generates JWT token
     - Stores token in Redis
     - Publishes "welcome email" event to RabbitMQ
   ↓
   Auth Service returns JWT to frontend
   ↓
   Notification Service receives email event, sends welcome email

2. USER BROWSES TEMPLATES
   Frontend: GET http://localhost:9090/api/v1/templates/free
   ↓
   API Gateway routes to Template Service
   ↓
   Template Service:
     - Queries database for free templates
     - Returns list
   ↓
   Frontend displays template gallery

3. USER SELECTS TEMPLATE AND CREATES RESUME
   Frontend: POST http://localhost:9090/api/v1/resumes
   Headers: Authorization: Bearer {JWT}
   Body: { title: "My Resume", templateId: 5 }
   ↓
   API Gateway:
     - Validates JWT
     - Extracts userId, plan from token
     - Adds headers: X-User-Id: 42, X-User-Plan: FREE
     - Routes to Resume Service
   ↓
   Resume Service:
     - Checks plan quota (FREE users max 3)
     - Creates new Resume
     - Returns resume object
   ↓
   Template Service (called by frontend):
     - POST /api/v1/templates/5/usage
     - Increments usage count
   ↓
   Frontend has resumeId, can now add sections

4. USER ADDS SECTIONS
   Frontend: POST http://localhost:9090/api/v1/sections/resume/1
   Body: { sectionType: "EXPERIENCE", title: "Work", content: {...} }
   ↓
   Resume Service creates sections
   ↓
   User now has a resume with content

5. USER USES AI FEATURES
   Frontend: POST http://localhost:9090/api/v1/ai/ats-check
   Body: { resumeId: 1, resumeContent: "...", jobDescription: "..." }
   ↓
   API Gateway routes to AI Service
   ↓
   AI Service:
     - Saves request to database (for history)
     - Calls Gemini API
     - Parses response
     - Returns ATS report
   ↓
   Frontend shows ATS score and suggestions

6. USER EXPORTS RESUME
   Frontend: POST http://localhost:9090/api/v1/exports
   Body: { resumeId: 1, format: "PDF" }
   ↓
   Export Service:
     - Creates export job
     - Queues on RabbitMQ
   ↓
   Export worker processes job asynchronously
   ↓
   Publishes notification event
   ↓
   Notification Service sends "ready" email
   ↓
   User downloads PDF
```

---

## Infrastructure & Tools

### Databases
- **MySQL**: Stores all persistent data (separate database per service)
- **Redis**: Stores active JWT tokens (for logout tracking)

### Message Queue
- **RabbitMQ**: Asynchronous messaging between services (notifications, exports)

### External APIs
- **Google Gemini**: AI features (ATS, tailor, generate)
- **Jooble**: Job listings aggregator
- **Razorpay**: Payment processing
- **Google OAuth**: Social login
- **SMTP (Gmail)**: Email sending

### Docker
All services run in Docker containers, orchestrated by `docker-compose.yml`

---

## Summary

Think of the backend like a restaurant:

- **Eureka** = Hostess who knows where all the staff is
- **API Gateway** = Front door where all customers enter
- **Auth Service** = Staff that checks if you're a customer and gives you a loyalty card
- **Template Service** = Menu designer with different menu styles
- **Resume Service** = Kitchen that prepares dishes (resumes)
- **AI Service** = Quality control that analyzes dishes
- **Export Service** = Packaging and delivery service
- **Notification Service** = Manager who calls customers with updates
- **Job Match Service** = Career advisor that matches customers with job opportunities

Each service does one job well, and they all communicate through clear channels (API Gateway for synchronous, RabbitMQ for asynchronous).
