# 🛒 Ecommerce Recipes Microservices

A scalable **Spring Boot Microservices-based E-commerce system** built with **Docker, Kafka, and API Gateway architecture**. This project demonstrates real-world backend system design with distributed services and event-driven communication.

---

## 🚀 Tech Stack

* **Backend:** Spring Boot, Java
* **Architecture:** Microservices
* **API Gateway:** Spring Cloud Gateway
* **Service Discovery:** Eureka Server
* **Messaging:** Apache Kafka
* **Database:**  PostgreSQL 
* **Containerization:** Docker & Docker Compose

---

## 🧩 Microservices Overview

| Service Name              | Description                              |
| ------------------------- | ---------------------------------------- |
| 🧑 User Service           | Handles user authentication & management |
| 📦 Product/Recipe Service | Manages products/recipes                 |
| 🛒 Order Service          | Handles order creation & processing      |
| 📊 Inventory Service      | Manages stock availability               |
| 💳 Payment Service        | Handles payments                         |
| 🔔 Notification Service   | Sends notifications using Kafka          |
| 🌐 API Gateway            | Routes all client requests               |
| 🧭 Service Registry       | Eureka server for service discovery      |

---

## 🏗️ Architecture

```text
Client
   ↓
API Gateway
   ↓
-----------------------------
|  Order Service            |
|  Inventory Service        |
|  User Service             |
|  Recipe Service           |
|  Payment Service          |
-----------------------------
   ↓
Kafka (Event Streaming)
   ↓
Notification Service
```

---

## ⚙️ How It Works

1. Client sends request via **API Gateway**
2. Gateway routes request to appropriate microservice
3. Services communicate via REST or Kafka events
4. Kafka handles asynchronous communication (e.g., order → notification)
5. Eureka manages service discovery dynamically

---

## 🐳 Running the Project with Docker

### 1️⃣ Clone the repository

```bash
git clone https://github.com/Ayushcoolkarni/Ecommerce-recpies--microservices.git
cd Ecommerce-recpies--microservices
```

---

### 2️⃣ Start all services

```bash
docker-compose up --build
```

---

### 3️⃣ Access services

* API Gateway: `http://localhost:8080`
* Eureka Dashboard: `http://localhost:8761`

---

## 📡 Example Flow

```text
Place Order →
Order Service →
Inventory Check →
Payment Processing →
Kafka Event →
Notification Service →
User gets confirmation
```

---

## 📁 Project Structure

```text
Ecommerce-recpies--microservices
 ├── api-gateway
 ├── service-registry
 ├── user-service
 ├── recipe-service
 ├── order-service
 ├── inventory-service
 ├── payment-service
 ├── notification-service
 ├── docker-compose.yml
```

---

## 🧠 Key Features

* 🔥 Microservices architecture (scalable & modular)
* ⚡ Event-driven communication using Kafka
* 🌐 Centralized API Gateway
* 🔍 Service discovery with Eureka
* 🐳 Fully containerized using Docker
* 📦 Independent deployable services

---

## 📌 Future Improvements

* Add centralized logging (ELK Stack)
* Add monitoring (Prometheus + Grafana)
* Implement CI/CD pipeline
* Add authentication (JWT / OAuth2)

---

## 👨‍💻 Author

**Ayush Kulkarni**

---

## ⭐ If you like this project

Give it a ⭐ on GitHub and feel free to fork & contribute!
