# VeriLearn

VeriLearn is a backend project for a verification-based self-learning system.

It focuses on one core question: how to turn learning into a process that can be planned, verified, recorded, and adjusted by the system, instead of only serving content.

This repository currently contains the backend MVP built with Spring Boot, centered on goal setup, persistence, API conventions, and the core data flow required for future task generation and validation.

## Table of Contents

- [Background](#background)
- [Project Status](#project-status)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Repository Structure](#repository-structure)
- [Quick Start](#quick-start)
- [API Overview](#api-overview)
- [Testing](#testing)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

## Background

Many learning products are good at providing resources, but weak at answering questions like:

- What should the learner study today?
- Has the learner really understood the content?
- How should the next step change based on the result?

VeriLearn is intended to support a workflow like this:

```text
Goal setup
  -> knowledge breakdown
  -> daily task generation
  -> learning validation
  -> result diversion
  -> next-step adjustment
```

The current version implements the backend foundation for that workflow, starting from the earliest stable part of the system:

```text
Submit learning goal
  -> find or create learner
  -> create or update goal
  -> persist into MySQL
  -> return unified JSON response
```

## Project Status

This project is in active MVP development.

What is already implemented:

- Spring Boot backend scaffold
- unified API response model
- global exception handling
- MySQL integration
- core table schema for learner, goal, and knowledge node
- goal creation and update API
- automated tests for controller and persistence paths

What is planned next:

- knowledge tree generation
- daily task generation
- learning validation flow
- progress query
- Feishu integration

## Features

Current backend capabilities include:

- `GET /ping`
  - health check endpoint for service and HTTP chain verification

- Unified API response
  - standard `code / message / data` response format

- Global exception handling
  - centralized error response for controller layer exceptions

- `POST /api/goals`
  - create or update a learning goal by `feishuOpenId`

- MySQL persistence
  - real database connectivity with tested read/write flow

- Core schema
  - `learner_user`
  - `learning_goal`
  - `knowledge_node`

## Architecture

The project currently follows a standard layered monolith structure:

```text
Controller
  -> Service
  -> Mapper
  -> MySQL
```

Example for the current goal flow:

```text
GoalController
  -> GoalServiceImpl
  -> LearnerUserMapper / LearningGoalMapper
  -> learner_user / learning_goal
```

This keeps responsibilities clear:

- Controller handles HTTP request and response
- Service handles business rules and flow
- Mapper handles persistence
- MySQL stores domain data

## Tech Stack

- Java 17
- Spring Boot 3.5.13
- Maven 3.9.x
- MySQL 8.x
- MyBatis-Plus 3.5.15
- JUnit 5

Selection principles:

- keep the project simple enough for a single developer MVP
- preserve clear structure for learning and interview presentation
- support future extension without introducing heavy infrastructure too early

## Repository Structure

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

Package responsibilities:

- `common`: shared response model, exception handling, health endpoint
- `user`: learner domain
- `goal`: learning goal domain
- `knowledge`: knowledge structure domain
- `task`: daily task domain placeholder
- `validation`: validation domain placeholder
- `progress`: progress query domain placeholder
- `ai`: model integration placeholder
- `infra`: infrastructure and third-party integration placeholder

## Quick Start

### Prerequisites

- JDK 17
- Maven 3.9.x
- MySQL 8.x

### 1. Create database

```sql
CREATE DATABASE verilearn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configure local database

Current local development configuration expects:

- database: `verilearn`
- username: `root`
- password: `root`

Main configuration file:

- `verilearn/src/main/resources/application.yml`

### 3. Run the application

```bash
cd verilearn
mvn spring-boot:run
```

### 4. Run tests

```bash
cd verilearn
mvn test
```

## API Overview

### Health Check

`GET /ping`

Response:

```text
VeriLearn API is running
```

### Create or Update Goal

`POST /api/goals`

Request example:

```json
{
  "feishuOpenId": "demo-user-001",
  "topic": "Java backend",
  "targetLevel": "intern",
  "dailyMinutes": 180
}
```

Response example:

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

## Testing

Current automated tests include:

- `DemoControllerTest`
  - verifies unified response and global exception handling

- `GoalControllerTest`
  - verifies goal creation and update flow

- `DatabaseSmokeTest`
  - verifies database connectivity and basic persistence operations

## Roadmap

Planned directions for the next iterations:

- generate structured knowledge nodes for a goal
- support daily task generation
- support validation question flow
- support progress query APIs
- integrate with Feishu bot callbacks

## Contributing

This repository is still evolving as an MVP.

If you want to contribute:

1. fork the repository
2. create a feature branch
3. keep changes focused and readable
4. add or update tests when behavior changes
5. open a pull request with a clear description

## License

No open source license has been added yet.
