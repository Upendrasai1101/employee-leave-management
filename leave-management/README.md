<p align="center">
  <img src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&weight=700&size=28&duration=4000&pause=1000&color=3366FF&center=true&vCenter=true&width=700&height=60&lines=Spring+Boot+3+REST+API;Employee+Leave+Management+System;Stateless+JWT+Auth+%26+Role-Based+Security" alt="Typing Banner" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025-orange?style=for-the-badge&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?style=for-the-badge&logo=springboot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Spring%20Security-6-red?style=for-the-badge&logo=springsecurity" alt="Spring Security">
  <img src="https://img.shields.io/badge/Hibernate-JPA-blue?style=for-the-badge&logo=hibernate" alt="Hibernate">
  <img src="https://img.shields.io/badge/MySQL-8.0-blueviolet?style=for-the-badge&logo=mysql" alt="MySQL">
  <img src="https://img.shields.io/badge/Maven-3.9-orange?style=for-the-badge&logo=apachemaven" alt="Maven">
</p>

---

## 📌 Overview
An enterprise-grade **Employee Leave Management System** built using **Spring Boot 3**[cite: 31, 38, 39]. This application handles the complete lifecycle of employee leave requests, featuring secure stateless **JWT authentication**, **Role-Based Access Control (RBAC)**, robust concurrency handling via **Optimistic Locking**, and a built-in lightweight Single-Page Web UI[cite: 31, 33, 34, 38].

---

## 🚀 Key Features & Architectural Highlights
- **Clean Layered Architecture:** Strictly decoupled into Controller $\rightarrow$ Service $\rightarrow$ Repository $\rightarrow$ Hibernate/JPA[cite: 38].
- **Stateless JWT Security:** Secure token-based authentication using `jjwt 0.12.6`, complete with custom `OncePerRequestFilter` and custom JSON security error handlers (401/403)[cite: 39, 42].
- **Concurrency Protection:** Uses `@Version` (Optimistic Locking) on the `Employee` entity to prevent race conditions and double-deductions during simultaneous manager approvals[cite: 33, 34].
- **Custom Business Rule Validations:** 
  - Overlap check via JPQL interval math to prevent booking duplicate dates[cite: 34, 41].
  - Server-calculated inclusive days (`numberOfDays`) to prevent client tampering[cite: 33, 40].
  - Automatic balance re-verification during approval time[cite: 34].
- **Built-in Web UI:** A zero-build, responsive single-page dashboard (`index.html`) served directly by Spring Boot supporting live auto-refreshing polling every 10 seconds[cite: 34, 44].
- **Comprehensive Testing:** 28 robust unit tests covering service business logic, JWT issuance/validation, custom date validation, and authentication rules using JUnit 5 and Mockito[cite: 31, 35, 45].

---

## 🛠️ Tech Stack
* **Language:** Java 17+ (Tested on Java 25 via compatibility flags)
* **Framework:** Spring Boot 3.3.5 (Spring Web, Spring Data JPA, Spring Security)[cite: 38, 39]
* **Database:** MySQL 8 (Production/Docker) & H2 File-Mode Profile (Zero-setup local development)[cite: 31, 39]
* **Validation & Security:** Jakarta Bean Validation, BCrypt Password Hashing, JJWT[cite: 39]
* **Testing & Tools:** JUnit 5, Mockito, AssertJ, Maven, Postman, VS Code REST Client (`.http`)[cite: 39, 45]

---

## 📂 Project Structure
```text
src/main/java/com/example/leave
├── controller/   AuthController, LeaveController          (HTTP endpoints only)
├── service/      AuthService, LeaveService                (Core business logic)
├── repository/   EmployeeRepository, LeaveRequestRepository (JPA Data access)
├── entity/       Employee, LeaveRequest, Role, LeaveType, LeaveStatus
├── dto/          Request/Response records, ErrorResponse
├── validation/   @ValidDateRange custom validator
├── exception/    Custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
├── security/     JwtService, JwtAuthenticationFilter, SecurityConfig, UserDetails...
└── config/       ClockConfig, ManagerSeeder               (Seeded admin & clock bean)
src/main/resources/application.yml        (Externalized DB & JWT configuration)
src/main/resources/application-h2.yml     (File-mode H2 local development profile)
src/main/resources/static/index.html      (Built-in responsive web dashboard UI)
postman/                                   (Postman collection + .http client file)
docker-compose.yml                         (MySQL container setup)
