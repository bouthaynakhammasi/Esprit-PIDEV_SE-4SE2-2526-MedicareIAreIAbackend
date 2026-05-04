# 🏥 MedicareIA Backend

## 📌 Overview
This is the backend of the **MedicareIA project**, a healthcare management system developed for academic purposes (PIDEV - ESPRIT).

It provides REST APIs for managing users, authentication, appointments, and medical services, and communicates with the frontend application.

---

## ⚙️ Technologies Used

- Java
- Spring Boot
- Spring Data JPA
- Spring Security (JWT Authentication)
- Hibernate
- Maven
- MySQL / PostgreSQL

---

## 🏗️ Architecture

The backend follows a layered architecture:

### Layers:
- **Controller Layer**: Handles HTTP requests (REST APIs)
- **Service Layer**: Business logic
- **Repository Layer**: Database operations
- **Entity Layer**: Data models

---

## 🔐 Security

- JWT Authentication
- Role-based access control:
  - Patient
  - Doctor
  - Admin

---

## 🚀 How to Run the Project

### 1. Clone repository
```bash
git clone https://github.com/bouthaynakhammasi/Esprit-PIDEV_SE-4SE2-2526-MedicareIAreIAbackend.git

Use IntelliJ IDEA / Eclipse

3. Configure database

Edit application.properties:

spring.datasource.url=jdbc:mysql://localhost:3306/medicareia
spring.datasource.username=root
spring.datasource.password=your_password
4. Run the project

Run the main class:

MedicareIaApplication.java
🌐 API Endpoints (Examples)
🔐 Authentication
POST /auth/register
POST /auth/login
👤 Users
GET /users
GET /users/{id}
DELETE /users/{id}
📅 Appointments
POST /appointments
GET /appointments
PUT /appointments/{id}
DELETE /appointments/{id}
🔗 Frontend Integration

This backend is consumed by the Angular/React frontend via REST API.

Base URL:
http://localhost:8080/api
