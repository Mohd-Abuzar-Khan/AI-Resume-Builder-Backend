# AI Resume Builder Backend

This repository contains the microservices for the AI Resume Builder application.

## Services in this branch

### 1. Eureka Server
The service registry that allows microservices to find and communicate with each other.

### 2. API Gateway
The entry point for all client requests, handling routing to the appropriate microservices.

### 3. Auth Service
Handles user authentication, registration, and security using JWT and Spring Security.

### 4. AI Service
Provides AI-powered features such as resume generation, tailoring, and ATS analysis using Gemini AI.

### 5. Resume Service
Manages user resumes, sections, and personal data.

### 6. Template Service
Provides a collection of resume templates for users to choose from.

### 7. Notification Service
Handles email and system notifications using RabbitMQ and SMTP.

### 8. Export Service
Manages exporting resumes to PDF/JSON and storage in S3.

### 9. Job Match Service
Matches user resumes with real-time job listings using Jooble and Gemini AI.

## Technologies
- Java 25
- Spring Boot
- Spring Cloud (Eureka, Gateway)
- Maven
