# 🛍️ SmartShop Backend

> **Java Spring Boot REST API with MySQL integration for the SmartShop E-Commerce Platform — a full-featured online retail and order management application.**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 📌 Project Description

SmartShop Backend is a **RESTful API** built with **Spring Boot 3.3.5** and **Java 21**. It powers the SmartShop e-commerce platform with secure endpoints for authentication, product management, shopping cart, order processing, chatbot conversations, support tickets, and admin operations.

The backend uses **MySQL** for persistent storage, **Spring Data JPA + Hibernate** for ORM, and **JJWT** for stateless JWT-based authentication. On startup, it automatically seeds sample categories, Indian retail products, and default accounts.

---

## ✨ Backend Features

| Feature | Details |
|---|---|
| 🔐 **JWT Authentication** | Stateless token-based auth; roles: ROLE_CUSTOMER, ROLE_ADMIN |
| 🗃️ **MySQL + JPA** | Automatic schema creation via Hibernate DDL |
| 🌱 **Database Seeder** | Auto-seeds admin/customer users, 6 categories, 17 products on startup |
| 📦 **Product Management** | CRUD, search, filter (keyword, category, brand, price), sort |
| 🗂️ **Category Management** | CRUD with product count validation before deletion |
| 🛒 **Cart Management** | Add/update/remove items, auto-recalculate totals, stock validation |
| 📋 **Order Management** | Place orders, status tracking, customer cancellation with inventory restore |
| 💬 **Chatbot Engine** | Intent-based conversation handling (product search, order lookup, FAQ) |
| 🎫 **Support Tickets** | Customer/admin ticket system with message threads |
| ❓ **FAQ Management** | CRUD with categories and sort order |
| 📊 **Admin Dashboard** | Aggregate stats: revenue, orders, users, products, order breakdown |
| 🔒 **Spring Security** | CORS configured, stateless sessions, JWT filter chain |

---

## 🛠️ Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| **Java** | 21 | Programming language |
| **Spring Boot** | 3.3.5 | Application framework |
| **Spring Data JPA** | (Boot managed) | ORM and database access |
| **Spring Security** | (Boot managed) | Authentication and authorization |
| **Spring Validation** | (Boot managed) | Request validation |
| **Hibernate** | (Boot managed) | JPA implementation |
| **MySQL** | 8.0 | Relational database |
| **JJWT** | 0.12.5 | JWT token generation and validation |
| **Lombok** | (Boot managed) | Boilerplate code reduction |
| **Apache Maven** | 3.x | Build tool |

---

## 📦 Dependencies (pom.xml)

```xml
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
mysql-connector-j
jjwt-api (0.12.5)
jjwt-impl (0.12.5)
jjwt-jackson (0.12.5)
lombok
spring-boot-starter-test
```

---

## 🏗️ Project Structure

```
src/
└── main/
    ├── java/com/smartshop/backend/
    │   ├── BackendApplication.java        # Spring Boot entry point
    │   │
    │   ├── config/
    │   │   ├── AppConfig.java             # PasswordEncoder bean
    │   │   ├── DatabaseSeeder.java        # Seeds initial data on startup
    │   │   └── SecurityConfig.java        # Spring Security + CORS configuration
    │   │
    │   ├── controller/
    │   │   ├── AuthController.java        # POST /api/auth/register, /api/auth/login
    │   │   ├── ProductController.java     # GET/POST/PUT/DELETE /api/products/**
    │   │   ├── CategoryController.java    # GET/POST/PUT/DELETE /api/categories/**
    │   │   ├── CartController.java        # GET/POST/PUT/DELETE /api/cart/**
    │   │   ├── OrderController.java       # GET/POST/PUT/PATCH /api/orders/**
    │   │   ├── UserController.java        # GET/PUT /api/users/**
    │   │   ├── ChatbotController.java     # POST /api/chatbot/**
    │   │   ├── SupportTicketController.java # GET/POST/PATCH /api/support/**
    │   │   ├── FAQController.java         # GET/POST/PUT/DELETE /api/faqs/**
    │   │   └── HomeController.java        # GET /api/test (health check)
    │   │
    │   ├── entity/                        # JPA entities (MySQL tables)
    │   │   ├── User.java
    │   │   ├── Product.java
    │   │   ├── Category.java
    │   │   ├── Cart.java / CartItem.java
    │   │   ├── Order.java / OrderItem.java
    │   │   ├── OrderStatus.java / PaymentMethod.java / PaymentStatus.java
    │   │   ├── ChatConversation.java / ChatMessage.java
    │   │   ├── SupportTicket.java / TicketMessage.java
    │   │   ├── FAQ.java
    │   │   ├── Address.java
    │   │   ├── Subcategory.java
    │   │   └── Role.java
    │   │
    │   ├── dto/                           # Request/Response DTOs (31 files)
    │   ├── repository/                    # Spring Data JPA repositories (13 files)
    │   ├── service/                       # Business logic interfaces + implementations
    │   ├── security/                      # JwtUtils, JwtAuthenticationFilter
    │   └── exception/                     # GlobalExceptionHandler, custom exceptions
    │
    └── resources/
        ├── application.properties         # App configuration (uses env vars)
        └── application-example.properties # Example config with placeholder values
```

---

## ✅ Prerequisites

- **Java 21** ([Eclipse Adoptium](https://adoptium.net/))
- **Apache Maven 3.8+** (or use the bundled Maven wrapper `mvnw`)
- **MySQL 8.0** (running locally)

---

## 🗄️ MySQL Database Setup

```sql
-- Connect to MySQL as root
mysql -u root -p

-- Create the database
CREATE DATABASE IF NOT EXISTS smartshop_db;
```

> The application uses `spring.jpa.hibernate.ddl-auto=update` which automatically creates and updates all tables on startup.

---

## 🔧 Environment Variable Configuration

The application reads configuration from `application.properties` using environment variable substitution.

| Variable | Description | Default (dev only) |
|---|---|---|
| `DB_USERNAME` | MySQL username | `root` |
| `DB_PASSWORD` | MySQL password | `root` |
| `APP_JWT_SECRET` | JWT signing secret (64+ hex chars) | insecure placeholder |

### Set Environment Variables (Windows PowerShell)

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
$env:APP_JWT_SECRET = "your_64_char_hex_secret_here"
```

### Set Environment Variables (Linux/macOS)

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
export APP_JWT_SECRET=your_64_char_hex_secret_here
```

> See `src/main/resources/application-example.properties` for the full configuration template.

---

## ▶️ Running the Backend

### Using Maven Wrapper (recommended)

```powershell
# Windows
.\mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

### Using Maven Directly

```bash
mvn spring-boot:run
```

### Build a JAR

```bash
mvn clean package -DskipTests
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

The server starts at **http://localhost:8080**

### 🌱 Default Seeded Accounts (auto-created on first startup)

| Role | Email | Password |
|---|---|---|
| Admin | `admin@smartshop.com` | `admin123` |
| Customer | `customer@smartshop.com` | `customer123` |

---

## 📡 API Endpoint Documentation

### Authentication — `/api/auth`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/api/auth/register` | Register a new customer | No |
| POST | `/api/auth/login` | Login and receive JWT | No |

### Products — `/api/products`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| GET | `/api/products` | List/filter/search products | No |
| GET | `/api/products/{id}` | Get product by ID | No |
| POST | `/api/products` | Create product | Admin |
| PUT | `/api/products/{id}` | Update product | Admin |
| DELETE | `/api/products/{id}` | Delete product | Admin |
| GET | `/api/products/low-stock` | List low-stock products | Admin |

**Filter params:** `search`, `categoryId`, `brand`, `minPrice`, `maxPrice`, `sort` (priceasc, pricedesc, rating, newest)

### Categories — `/api/categories`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| GET | `/api/categories` | List all categories with product count | No |
| GET | `/api/categories/{id}` | Get category by ID | No |
| POST | `/api/categories` | Create category | Admin |
| PUT | `/api/categories/{id}` | Update category | Admin |
| DELETE | `/api/categories/{id}` | Delete category | Admin |

### Cart — `/api/cart`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| GET | `/api/cart?userId=1` | Get cart by user ID | Yes |
| GET | `/api/cart?email=user@x.com` | Get cart by email | Yes |
| POST | `/api/cart/items` | Add item to cart | Yes |
| PUT | `/api/cart/items/{id}` | Update cart item quantity | Yes |
| DELETE | `/api/cart/items/{id}` | Remove cart item | Yes |
| DELETE | `/api/cart/clear?userId=1` | Clear entire cart | Yes |

### Orders — `/api/orders`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/api/orders` | Place a new order | Yes |
| GET | `/api/orders/{id}` | Get order by ID | Yes |
| GET | `/api/orders/number/{orderNumber}` | Get order by order number | Yes |
| GET | `/api/orders/user/{userId}` | Get orders by user | Yes |
| GET | `/api/orders` | Get all orders | Admin |
| PUT | `/api/orders/{id}/status` | Update order status | Admin |
| PATCH | `/api/orders/{id}/cancel` | Cancel an order | Yes |
| GET | `/api/orders/admin/stats` | Dashboard stats | Admin |

### Users — `/api/users`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| GET | `/api/users/profile?email=x` | Get current user profile | Yes |
| PUT | `/api/users/profile?email=x` | Update profile | Yes |
| GET | `/api/users` | List all users | Admin |
| PUT | `/api/users/{id}/status` | Activate/deactivate user | Admin |

### Chatbot — `/api/chatbot`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/api/chatbot/message` | Send message to chatbot | No |
| GET | `/api/chatbot/conversations/{id}` | Get conversation detail | Yes |
| GET | `/api/chatbot/admin/conversations` | All conversations | Admin |
| POST | `/api/chatbot/admin/conversations/{id}/join` | Admin joins chat | Admin |
| POST | `/api/chatbot/admin/conversations/{id}/message` | Admin sends message | Admin |

### Support Tickets — `/api/support`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| POST | `/api/support/tickets` | Create ticket | Yes |
| GET | `/api/support/tickets/my` | Get own tickets | Yes |
| GET | `/api/support/tickets/my/{id}` | Get own ticket detail | Yes |
| POST | `/api/support/tickets/my/{id}/messages` | Reply to ticket | Yes |
| GET | `/api/support/admin/tickets` | All tickets | Admin |
| GET | `/api/support/admin/tickets/{id}` | Any ticket detail | Admin |
| PATCH | `/api/support/admin/tickets/{id}` | Update ticket status | Admin |
| POST | `/api/support/admin/tickets/{id}/messages` | Admin reply | Admin |
| DELETE | `/api/support/admin/tickets/{id}` | Delete ticket | Admin |
| GET | `/api/support/admin/dashboard` | Support dashboard stats | Admin |

### FAQs — `/api/faqs`

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| GET | `/api/faqs` | List active FAQs | No |
| GET | `/api/faqs/category/{cat}` | Get FAQs by category | No |
| GET | `/api/faqs/{id}` | Get FAQ by ID | No |
| GET | `/api/faqs/admin/all` | List all FAQs | Admin |
| POST | `/api/faqs/admin` | Create FAQ | Admin |
| PUT | `/api/faqs/admin/{id}` | Update FAQ | Admin |
| DELETE | `/api/faqs/admin/{id}` | Delete FAQ | Admin |

---

## 🔐 Authentication

The API uses **JWT Bearer Token** authentication.

1. Call `POST /api/auth/login` with email and password
2. Receive a JWT token in the response body under `data.token`
3. Include the token in subsequent requests:

```http
Authorization: Bearer <your-jwt-token>
```

Tokens expire after **24 hours** (86400000 ms).

---

## 🧪 Testing

```bash
# Run all tests
.\mvnw.cmd test          # Windows
./mvnw test              # Linux/macOS

# Run with Maven directly
mvn test
```

> Integration tests require a running MySQL instance with `smartshop_db` database.

---

## 🔗 Frontend Repository

👉 **[SmartShop-Frontend](https://github.com/YOUR_USERNAME/SmartShop-Frontend)**

The React + Vite frontend connects to this backend API and provides the complete customer and admin UI.

---

## 👤 Contributors

| Name | Role |
|---|---|
| **SmartShop Team** | Full-Stack Developer |

---

*SmartShop – E-Commerce Platform for Online Retail and Order Management*
