# 🏋️ Smart Fit AI

> AI-powered Microservices Health Recommendation Platform built with Spring Boot, Spring Cloud, RabbitMQ, PostgreSQL, MongoDB, and Google Gemini AI.

---

## Overview

Smart Fit AI is a distributed fitness platform that demonstrates modern backend engineering principles using a microservices architecture.

The application collects user activity data, processes fitness metrics asynchronously, and generates AI-powered personalized health recommendations.

The project was built to explore:

- Microservices Architecture
- Event-Driven Communication
- Service Discovery
- API Gateway Pattern
- AI Integration
- Database-per-Service Pattern

---

## Problem Statement

Traditional monolithic fitness applications tightly couple user management, activity tracking, and recommendation logic.

This project separates these responsibilities into independent services, allowing each service to evolve and scale independently while communicating through asynchronous messaging.

---

# Architecture

```text
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ API Gateway │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│ Eureka Service      │
│ Discovery           │
└─────────────────────┘

       │
       ├─────────────────────────┐
       │                         │
       ▼                         ▼

┌─────────────┐         ┌─────────────┐
│ User Service│         │Activity Svc │
└──────┬──────┘         └──────┬──────┘
       │                       │
       ▼                       ▼

 PostgreSQL             MongoDB

                               │
                               ▼

                         RabbitMQ Queue

                               │
                               ▼

                        ┌─────────────┐
                        │ AI Service  │
                        └──────┬──────┘
                               │
                               ▼

                         Gemini AI API

                               │
                               ▼

                            MongoDB
```
---

## Services

### User Service

Responsible for:

- User Registration
- User Validation
- Profile Management

Database

- PostgreSQL

---

### Activity Service

Responsible for:

- Activity Tracking
- Activity History
- Publishing Activity Events

Database

- MongoDB

---

### AI Service

Responsible for:

- Consuming RabbitMQ Events
- Communicating with Gemini AI
- Generating Recommendations

Database

- MongoDB

---

## Engineering Decisions

### Why Microservices?

Separated business domains into independent services to reduce coupling and improve maintainability.

### Why RabbitMQ?

Recommendation generation does not need to block activity creation.

RabbitMQ enables asynchronous processing between Activity Service and AI Service.

### Why PostgreSQL?

User information is relational and transactional, making PostgreSQL a better fit.

### Why MongoDB?

Activity records and AI recommendations are document-oriented and evolve frequently.

---

## Technology Stack

### Backend

- Java 21
- Spring Boot 3
- Spring Cloud

### Infrastructure

- Eureka Server
- Config Server
- API Gateway

### Messaging

- RabbitMQ

### Databases

- PostgreSQL
- MongoDB

### AI

- Google Gemini API

---

## Request Flow

1. User registers.
2. User submits activity.
3. Activity Service validates the user.
4. Activity stored in MongoDB.
5. Activity event published to RabbitMQ.
6. AI Service consumes the event.
7. Gemini generates recommendations.
8. Recommendation stored.
9. Client retrieves recommendations.

---

## API Overview

### User Service

POST /api/users/register

GET /api/users/{id}

---

### Activity Service

POST /api/activities

GET /api/activities

---

### AI Service

GET /api/recommendations/user/{id}

---

## Project Structure

```
Smart-Fit-AI
│
├── gateway
├── eureka
├── configserver
├── userservice
├── activityservice
└── aiservice
```

---

## Running Locally

Prerequisites

- Java 21
- PostgreSQL
- MongoDB
- RabbitMQ

Startup Order

1. PostgreSQL
2. MongoDB
3. RabbitMQ
4. Config Server
5. Eureka
6. Gateway
7. User Service
8. Activity Service
9. AI Service

---

## Production Improvements

If this project were deployed to production, the following improvements would be added:

- JWT Authentication
- Docker Compose
- Kubernetes
- Prometheus & Grafana
- OpenAPI Documentation
- Distributed Tracing
- Rate Limiting
- CI/CD Pipeline

---

## Challenges

- Managing communication across multiple services
- Coordinating asynchronous workflows
- Integrating AI responses into an event-driven pipeline

---

## Lessons Learned

This project helped strengthen my understanding of:

- Spring Cloud
- Event-driven Architecture
- RabbitMQ
- API Gateway Pattern
- AI Integration
- Distributed Systems Fundamentals

---

## Author

**Lokesh Siddi**

GitHub: https://github.com/LokeshSiddi
