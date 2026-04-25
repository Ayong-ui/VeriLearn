# VeriLearn

VeriLearn is a verification-oriented learning backend project built with Java and Spring Boot.

The current version focuses on building a solid backend foundation for an AI-assisted learning workflow:

- set a learning goal
- persist user and goal data
- prepare knowledge nodes for later task generation
- keep the project structure clean and easy to explain in interviews

## Current Status

The repository currently contains a single Spring Boot application under [`verilearn/`](./verilearn).

Already implemented:

- project bootstrap with Spring Boot
- health check endpoint: `GET /ping`
- unified API response structure
- global exception handler
- demo API endpoints for response/error handling
- MySQL integration with MyBatis-Plus
- core tables:
  - `learner_user`
  - `learning_goal`
  - `knowledge_node`
- goal upsert API:
  - `POST /api/goals`
- automated tests for:
  - web response structure
  - goal creation/update flow
  - database smoke test

## Tech Stack

- Java 17
- Spring Boot 3.5.13
- Maven 3.9.x
- MySQL 8.x
- MyBatis-Plus 3.5.15
- JUnit 5

## Project Structure

```text
VeriLearn/
├─ README.md
├─ .gitignore
└─ verilearn/
   ├─ pom.xml
   ├─ mvnw
   ├─ mvnw.cmd
   ├─ .mvn/
   └─ src/
      ├─ main/
      │  ├─ java/com/verilearn/
      │  │  ├─ common/
      │  │  ├─ user/
      │  │  ├─ goal/
      │  │  ├─ knowledge/
      │  │  ├─ task/
      │  │  ├─ validation/
      │  │  ├─ progress/
      │  │  ├─ ai/
      │  │  └─ infra/
      │  └─ resources/
      └─ test/
```

## Key Endpoints

### 1. Health Check

`GET /ping`

Response:

```text
VeriLearn API is running
```

### 2. Demo API

`GET /api/demo`

Example response:

```json
{
  "code": 0,
  "message": "demo endpoint is ready",
  "data": {
    "module": "common",
    "status": "ready"
  }
}
```

### 3. Goal Upsert API

`POST /api/goals`

Request body:

```json
{
  "feishuOpenId": "demo-user-001",
  "topic": "Java backend",
  "targetLevel": "intern",
  "dailyMinutes": 180
}
```

Example response:

```json
{
  "code": 0,
  "message": "goal saved successfully",
  "data": {
    "userId": 1,
    "goalId": 1,
    "feishuOpenId": "demo-user-001",
    "topic": "Java backend",
    "targetLevel": "intern",
    "dailyMinutes": 180,
    "status": "ACTIVE"
  }
}
```

## Local Setup

### 1. Prepare MySQL

Create a local database:

```sql
CREATE DATABASE verilearn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Current local development configuration in `application.yml` uses:

- database: `verilearn`
- username: `root`
- password: `root`

You should change these values for your own environment if needed.

### 2. Run the application

From the `verilearn/` directory:

```bash
mvn spring-boot:run
```

or using Maven Wrapper:

```bash
./mvnw spring-boot:run
```

### 3. Run tests

```bash
mvn test
```

## Notes

- Development diaries and process notes are intentionally excluded from this repository.
- The current project is a modular single application by package, not a Maven multi-module aggregate project.
- The next planned step is to connect goal setting with knowledge node generation.
