# AI Resume Builder Backend

This repository contains the microservices for the AI Resume Builder application.

## Services in this branch

### 1. Eureka Server
The service registry that allows microservices to find and communicate with each other.

### 2. API Gateway
The entry point for all client requests, handling routing to the appropriate microservices.

### 3. Auth Service
Handles user authentication, registration, and security using JWT and Spring Security.

## Technologies
- Java 25
- Spring Boot
- Spring Cloud (Eureka, Gateway)
- Maven
