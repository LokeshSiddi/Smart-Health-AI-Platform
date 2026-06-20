# Smart-Fit AI

AI-powered fitness tracking platform built using Spring Boot microservices. The system collects user activity data, processes fitness metrics, and generates personalized health recommendations using Google's Gemini AI.

---

## Overview

Smart-Fit AI demonstrates a production-style microservices architecture using Spring Cloud components, asynchronous communication with RabbitMQ, service discovery with Eureka, centralized configuration management, and AI-powered recommendation generation.

The platform consists of independent services that collaborate to manage users, track fitness activities, and generate personalized recommendations.

---

## Architecture

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

## Features

### User Management

- User registration
- User profile retrieval
- User validation service
- PostgreSQL persistence

### Activity Tracking

- Track fitness activities
- Store activity records
- Retrieve activity history
- MongoDB storage

### AI Recommendations

- Personalized fitness recommendations
- Activity-based health insights
- Gemini AI integration
- Recommendation history storage

### Microservices Infrastructure

- Spring Cloud Config Server
- Eureka Service Discovery
- API Gateway
- RabbitMQ Event Messaging
- Distributed configuration management

---

## Tech Stack

### Backend

- Java 21
- Spring Boot 3
- Spring Cloud

### Infrastructure

- Spring Cloud Gateway
- Eureka Server
- Config Server

### Messaging

- RabbitMQ

### Databases

- PostgreSQL
- MongoDB

### AI

- Google Gemini API

### Build Tool

- Maven

---

## Services

### User Service

Handles user registration and profile management.

#### Endpoints

```http
POST /api/users/register
```

Register a new user.

```http
GET /api/users/{userId}
```

Retrieve user profile.

```http
GET /api/users/{userId}/validate
```

Validate user existence.

---

### Activity Service

Handles activity tracking and fitness data processing.

#### Endpoints

```http
POST /api/activities
```

Track a fitness activity.

```http
GET /api/activities
```

Retrieve user activities.

```http
GET /api/activities/{activityId}
```

Retrieve specific activity.

---

### AI Service

Generates AI-powered recommendations.

#### Endpoints

```http
GET /api/recommendations/user/{userId}
```

Get all recommendations for a user.

```http
GET /api/recommendations/activity/{activityId}
```

Get recommendation for a specific activity.

---

## Configuration Services

### Eureka Server

Service discovery and registration.

**Port:** `8761`

### Config Server

Centralized configuration management.

**Port:** `8888`

### API Gateway

Single entry point for all services.

---

## Databases

| Service | Database |
|----------|----------|
| User Service | PostgreSQL |
| Activity Service | MongoDB |
| AI Service | MongoDB |

---

## Event Flow

1. User registers through User Service.
2. User submits activity data.
3. Activity Service validates the user.
4. Activity data is stored in MongoDB.
5. Activity event is published to RabbitMQ.
6. AI Service consumes the event.
7. Gemini AI generates recommendations.
8. Recommendations are stored and exposed through APIs.

---

## Project Structure

```text
Smart-Fit-AI
│
├── userservice
├── activityservice
├── aiservice
├── gateway
├── configserver
└── eureka
```

---

## Running the Project

### Prerequisites

- Java 21
- Maven
- PostgreSQL
- MongoDB
- RabbitMQ
- Gemini API Key

### Startup Order

```text
1. PostgreSQL
2. MongoDB
3. RabbitMQ
4. Config Server
5. Eureka Server
6. API Gateway
7. User Service
8. Activity Service
9. AI Service
```

---

## Environment Variables

```env
GEMINI_API_KEY=your_api_key
GEMINI_API_URL=your_gemini_endpoint
```

---

## Learning Outcomes

This project demonstrates:

- Microservices Architecture
- Service Discovery
- API Gateway Pattern
- Event-Driven Communication
- Distributed Configuration
- AI Integration
- Database-per-Service Pattern
- Spring Cloud Ecosystem

---

## Future Improvements

- Docker Compose Deployment
- Kubernetes Support
- JWT Authentication
- Prometheus & Grafana Monitoring
- OpenAPI Documentation
- CI/CD Pipeline
- Distributed Tracing
- Rate Limiting
- Health Monitoring

---

## Author

**Lokesh Siddi**

GitHub: https://github.com/LokeshSiddi
