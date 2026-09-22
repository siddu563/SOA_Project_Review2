# LEARNKNOW Postman Testing Guide

Use the API Gateway for normal testing:

```text
http://localhost:8080
```

Current environment note: port `8080` is occupied by an unrelated Jetty process and must be released safely before starting the LEARNKNOW gateway. Do not kill PID `7836` blindly; inspect it with `Get-CimInstance Win32_Process -Filter "ProcessId = 7836"` and stop it only after confirming ownership.

## Prerequisites

1. Start PostgreSQL on port `5432`.
2. Configure credentials in the PowerShell session used to start the services. Do not commit these values:

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your-postgres-password"
$env:JWT_SECRET = "your-development-secret-at-least-32-characters"
```

3. Create the databases if they do not already exist using [database_setup.sql](database_setup.sql) in pgAdmin. The six databases are `knowhub_auth`, `knowhub_course`, `knowhub_access`, `knowhub_assignment`, `knowhub_usage`, and `knowhub_ai`.

```sql
-- PostgreSQL CREATE DATABASE statements are in database_setup.sql.
```

4. Start services in this order: Eureka, Auth, Course, Access, Assignment, Usage, AI, then API Gateway.
5. In Postman, create an environment variable named `baseUrl` with value `http://localhost:8080`, and an empty variable named `jwt`.

## 1. Register

**Request**

```http
POST {{baseUrl}}/api/auth/register
Content-Type: application/json
```

```json
{
  "name": "Postman Student",
  "email": "postman.student@example.com",
  "password": "Use-a-local-test-password",
  "role": "STUDENT"
}
```

**Expected:** `201 Created`. The response contains `id`, `name`, `email`, `role`, and `createdAt`; it does not contain the password.

## 2. Login and Save JWT

**Request**

```http
POST {{baseUrl}}/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "postman.student@example.com",
  "password": "Use-a-local-test-password"
}
```

**Expected:** `200 OK` with `token` and user details. Do not log or share the token outside the local test.

In the Tests tab, save the token:

```javascript
const body = pm.response.json();
pm.environment.set("jwt", body.token);
```

For subsequent requests, use Postman's **Authorization > Bearer Token** with value `{{jwt}}`. The raw HTTP header is:

```http
Authorization: Bearer {{jwt}}
```

## 3. Get Courses

```http
GET {{baseUrl}}/api/courses
Authorization: Bearer {{jwt}}
```

**Expected:** `200 OK` with an array of courses.

## 4. Create a Course

Use an admin JWT for this operation when role policies are enforced by the deployment.

```http
POST {{baseUrl}}/api/courses
Authorization: Bearer {{jwt}}
Content-Type: application/json
```

```json
{
  "title": "Java Fundamentals",
  "description": "Variables, classes, collections, and exceptions",
  "level": "BEGINNER"
}
```

**Expected:** `201 Created`. Save the returned `id` as `courseId`.

## 5. Enroll the User

```http
POST {{baseUrl}}/api/access/enroll
Authorization: Bearer {{jwt}}
Content-Type: application/json
```

```json
{
  "userId": 1,
  "courseId": 1
}
```

**Expected:** `201 Created` with an `ACTIVE` enrollment. Replace IDs with the values returned by the register and course requests.

## 6. Check Enrollment and Access

```http
GET {{baseUrl}}/api/access/check/1/1
Authorization: Bearer {{jwt}}
```

**Expected:** `200 OK` with `enrolled: true` for an active enrollment.

## 7. Get Assignments

```http
GET {{baseUrl}}/api/assignments/course/1
Authorization: Bearer {{jwt}}
```

**Expected:** `200 OK` with an assignment array.

## 8. Create an Assignment for Testing

```http
POST {{baseUrl}}/api/assignments
Authorization: Bearer {{jwt}}
Content-Type: application/json
```

```json
{
  "courseId": 1,
  "title": "Hello World",
  "description": "Submit a Java Hello World program",
  "dueDate": "2026-12-31T23:59:59"
}
```

**Expected:** `201 Created`. Save the returned assignment `id`.

## 9. Submit an Assignment

```http
POST {{baseUrl}}/api/assignments/1/submit
Authorization: Bearer {{jwt}}
Content-Type: application/json
```

```json
{
  "userId": 1,
  "code": "public class Main { public static void main(String[] args) { System.out.println(\"Hello World\"); } }"
}
```

**Expected:** `200 OK` with a persisted submission and `SUBMITTED` status.

## 10. Record Usage Activity

```http
POST {{baseUrl}}/api/usage/activity
Authorization: Bearer {{jwt}}
Content-Type: application/json
```

```json
{
  "userId": 1,
  "resourceId": 1,
  "action": "READ"
}
```

**Expected:** `200 OK` with the saved usage record.

## 11. Update and Check Progress

```http
POST {{baseUrl}}/api/usage/progress
Authorization: Bearer {{jwt}}
Content-Type: application/json
```

```json
{
  "userId": 1,
  "courseId": 1,
  "moduleId": 1,
  "completionPercent": 50
}
```

**Expected:** `200 OK`. Completion must be between `0` and `100`.

```http
GET {{baseUrl}}/api/usage/progress/1/1
Authorization: Bearer {{jwt}}
```

**Expected:** `200 OK` with the saved progress.

## 12. Ask a Programming Doubt

```http
POST {{baseUrl}}/api/ai/ask
Authorization: Bearer {{jwt}}
Content-Type: application/json
```

```json
{
  "userId": 1,
  "question": "What is a Java NullPointerException?"
}
```

**Expected:** `200 OK` with a deterministic programming explanation and persisted interaction ID.

## 13. Unauthorized Request

```http
GET {{baseUrl}}/api/courses
```

Remove the Authorization header entirely.

**Expected:** `401 Unauthorized` or `403 Forbidden` from the gateway.

Repeat with an invalid or expired bearer token. The gateway should reject it before forwarding the request.

## Troubleshooting

- Eureka dashboard: `http://localhost:8761`
- Eureka registry: `http://localhost:8761/eureka/apps`
- Expected service names: `API-GATEWAY`, `AUTH-SERVICE`, `COURSE-SERVICE`, `ACCESS-SERVICE`, `ASSIGNMENT-SERVICE`, `USAGE-SERVICE`, and `AI-SERVICE`.
- If a service exits with a PostgreSQL authentication error, set `DB_USERNAME` and `DB_PASSWORD` in the same terminal before starting it. Both values are required; the application has no database credential fallback.
- Do not use individual service ports for normal Postman testing; use them only for isolated debugging after the gateway and Eureka path is working.
