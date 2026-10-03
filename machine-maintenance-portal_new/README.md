# machine-maintenance-portal1
# Dockerized Machine Maintenance Portal

A web-based **Machine Maintenance Portal** developed using Java, Spring Boot, Maven, Spring Data JPA, and MySQL. The system helps manage machine records and track their maintenance status.

## Features

* Create machine records
* View machine records
* Update machine records
* Search machine records
* Track machine status
* Summary dashboard
* MySQL database integration
* Git and GitHub version control
* Planned Docker containerization
* Planned Jenkins CI/CD integration

## Technology Stack

* **Backend:** Java, Spring Boot
* **Build Tool:** Maven
* **Database:** MySQL
* **ORM:** Spring Data JPA / Hibernate
* **Server:** Apache Tomcat
* **Version Control:** Git and GitHub
* **Containerization:** Docker
* **CI/CD:** Jenkins

## Machine Information

The portal maintains the following information:

* Machine ID
* Machine Code
* Machine Name
* Machine Type
* Location
* Status

### Machine Status

```text
OPERATIONAL
MAINTENANCE
UNDER_REPAIR
OUT_OF_SERVICE
```

## Project Structure

```text
machine-maintenance-portal/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── saroj/
│   │   │           └── machine_maintenance_portal/
│   │   │               ├── controller/
│   │   │               ├── model/
│   │   │               ├── repository/
│   │   │               └── MachineMaintenancePortalApplication.java
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .gitignore
└── README.md
```

## Database Setup

Create the MySQL database:

```sql
CREATE DATABASE machine_maintenance;
```

The main table used by the application is:

```text
machines
```

## Running the Application

### Clone the repository

```bash
git clone https://github.com/Saroj-Kadnor/machine-maintenance-portal.git
cd machine-maintenance-portal
```

### Run with Maven Wrapper

For Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The application runs at:


http://localhost:8080
```

## Git Workflow

The project follows a feature-based Git workflow:

main
  │
  └── develop
       │
       ├── feature/machine-management
       ├── feature/mvp-functions
       └── bugfix/<name>

