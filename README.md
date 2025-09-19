Smart Health AI Platform 🩺🤖
=============================

A cutting-edge, microservices-based platform that transforms wearable device data into personalized, AI-driven health insights. This project is built using **Java**, **Spring Boot**, and **Spring Cloud** to create a scalable and resilient system.

✨ Overview
----------

The **Smart Health AI Platform** is an intelligent system designed for the modern health-conscious individual. It seamlessly collects vital metrics from smartwatches, processes this data through a robust, event-driven architecture, and leverages the power of the **Gemini 2.0 Flash API** to deliver actionable, real-time health advice.

From personalized diet plans to critical safety alerts based on your vitals, our platform acts as your personal health companion.

🏗️ Architecture
----------------

The platform is built on a modern **Microservices Architecture** using the **Spring Cloud** ecosystem. This design decouples services, allowing for independent scaling and deployment, which enhances the system's overall resilience and flexibility.

*   **Config Server**: A centralized service for managing the external configuration for all microservices.
    
*   **Eureka Server**: Provides service discovery, allowing services to find and communicate with each other dynamically.
    
*   **API Gateway**: A single entry point for all client requests. It routes requests to the appropriate microservice and handles cross-cutting concerns like security and rate limiting.
    
*   **User Service**: Manages user profiles, authentication, and personal data.
    
*   **Activity Service**: Responsible for ingesting and processing fitness data from wearable devices.
    
*   **AI Service**: Integrates with the Google Gemini API to generate personalized health insights, tips, and recommendations.
    

🛠️ Tech Stack
--------------

Our platform leverages a modern, robust technology stack to deliver a seamless experience.

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/postgresql-%23336791.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-%234ea94b.svg?style=for-the-badge&logo=mongodb&logoColor=white)
![REST API](https://img.shields.io/badge/REST-API-%23000.svg?style=for-the-badge)
![OAuth2](https://img.shields.io/badge/OAuth2-2684FF?style=for-the-badge&logo=oauth&logoColor=white)
![Keycloak](https://img.shields.io/badge/keycloak-%233F72DA.svg?style=for-the-badge&logo=keycloak&logoColor=white)
![Microservices](https://img.shields.io/badge/Microservices-%2300BC7D.svg?style=for-the-badge)
![Event Messaging](https://img.shields.io/badge/Event%20Messaging-%232ea44f.svg?style=for-the-badge)
![RabbitMQ](https://img.shields.io/badge/rabbitmq-%FF6600.svg?style=for-the-badge&logo=rabbitmq&logoColor=white)
![Gemini API](https://img.shields.io/badge/Gemini-API-%23488EF1.svg?style=for-the-badge)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-000000.svg?style=for-the-badge&logo=intellij-idea&logoColor=white)
![Swagger](https://img.shields.io/badge/swagger-%2385EA2D.svg?style=for-the-badge&logo=swagger&logoColor=black)
![Postman](https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white)

📁 Project Structure
--------------------

The repository is a monorepo containing all the backend microservices.


Smart-Health-AI-Platform/  
├── activityservice/ # Manages user fitness activities  
├── aiservice/       # Handles AI-powered insight generation  
├── configserver/    # Centralized configuration management  
├── eureka/          # Service discovery registry  
├── gateway/         # API Gateway for routing requests  
└── userservice/     # Manages user data and profiles

🏁 Getting Started
------------------

Follow these instructions to get the project up and running on your local machine.

### Prerequisites

*   Java JDK 24+
    
*   Apache Maven 3.8+
    
*   A running instance of PostgreSQL, RabbitMQ & Keycloak
    
*   An API key for the Gemini API
    

### Installation & Setup

1.  **Clone the repository:**
    
2.  Example for userservice.properties:
    
3.  **Build All Modules:**Run the Maven command from the root directory to build all microservices.
    
4.  Alternatively, you can run each service manually. **The startup order is important!**
    
5.  **Access the Application:**
    
    *   **API Gateway**: http://localhost:8080 (or the port configured for the gateway)
        
    *   **Eureka Dashboard**: http://localhost:8761
        

🤝 Contributing
---------------

Contributions are what make the open-source community an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**. Please fork the repo and create a pull request.

1.  Fork the Project
    
2.  Create your Feature Branch (git checkout -b feature/AmazingFeature)
    
3.  Commit your Changes (git commit -m 'Add some AmazingFeature')
    
4.  Push to the Branch (git push origin feature/AmazingFeature)
    
5.  Open a Pull Request
    

📄 License
----------

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
