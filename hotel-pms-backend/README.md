# Hotel PMS Backend

Spring Boot backend for Hotel Property Management System.

## Setup

1. Install PostgreSQL and create database:
```sql
CREATE DATABASE hotel_pms;
```

2. Update `application.properties` with your database credentials

3. Build and run:
```bash
mvn clean install
mvn spring-boot:run
```

The API will be available at http://localhost:8085

## Default Users

The application creates default users on startup:
- Admin: `admin` / `admin123`
- Receptionist: `receptionist` / `admin123`

## API Endpoints

- `/api/auth/login` - Authentication
- `/api/rooms` - Room management
- `/api/bookings` - Booking management
- `/api/payments` - Payment processing
- `/api/reports` - Reports
- `/api/guests` - Guest management

## Fiscal Integration

The DatecsService is a mock implementation. Replace with actual Datecs SDK integration for production use.

