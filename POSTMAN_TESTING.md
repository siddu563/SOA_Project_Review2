# LEARNKNOW - Postman Testing & Viva Demonstration Guide
> **Review 2 Evaluation Guide: End-to-End API Workflows**

This guide provides step-by-step instructions for testing the LEARNKNOW microservices architecture via Postman. All requests route through the **API Gateway** on port `8080`.

---

## 1. Quick Start: Import Pre-built Collection

1. Open Postman.
2. Click **Import** (top left).
3. Select or drag-and-drop the included file:
   ```text
   LEARNKNOW_Review2.postman_collection.json
   ```
4. The collection is pre-configured with collection variables:
   - `baseUrl`: `http://localhost:8080`
   - `studentToken`: *(automatically populated upon student login)*
   - `adminToken`: *(automatically populated upon admin login)*
   - `courseId`: `1`
   - `assignmentId`: `1`

---

## 2. Environment Setup

If you prefer testing manually or creating an environment in Postman:

| Variable | Initial Value | Current Value |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | `http://localhost:8080` |
| `studentToken` | *(leave empty)* | *(populated after Step 2)* |
| `adminToken` | *(leave empty)* | *(populated after Step 3)* |
| `courseId` | `1` | *(updated after Step 7)* |
| `assignmentId` | `1` | *(updated after Step 11)* |

---

## 3. Step-by-Step API Execution Runbook

### Phase A: Authentication & Role Setup

#### 1. Register Student User
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/auth/register`
- **Headers:** `Content-Type: application/json`
- **Body:**
  ```json
  {
    "name": "Alice Student",
    "email": "alice@learnknow.io",
    "password": "password123!",
    "role": "STUDENT"
  }
  ```
- **Expected Status:** `201 Created`
- **Response:** Sanitized JSON containing `id`, `name`, `email`, `role`, and `createdAt` (password is never returned).

#### 2. Register Admin / Instructor User
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/auth/register`
- **Body:**
  ```json
  {
    "name": "Dr. Bob Admin",
    "email": "admin@learnknow.io",
    "password": "adminPassword123!",
    "role": "ADMIN"
  }
  ```
- **Expected Status:** `201 Created`

#### 3. Login as Student (Acquires `studentToken`)
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/auth/login`
- **Body:**
  ```json
  {
    "email": "alice@learnknow.io",
    "password": "password123!"
  }
  ```
- **Expected Status:** `200 OK`
- **Postman Tests Tab Script:**
  ```javascript
  const res = pm.response.json();
  pm.collectionVariables.set("studentToken", res.token);
  ```

#### 4. Login as Admin (Acquires `adminToken`)
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/auth/login`
- **Body:**
  ```json
  {
    "email": "admin@learnknow.io",
    "password": "adminPassword123!"
  }
  ```
- **Expected Status:** `200 OK`
- **Postman Tests Tab Script:**
  ```javascript
  const res = pm.response.json();
  pm.collectionVariables.set("adminToken", res.token);
  ```

---

### Phase B: Gateway Security & Negative Validations

#### 5. NEGATIVE: Login with Bad Password (401 Unauthorized)
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/auth/login`
- **Body:**
  ```json
  {
    "email": "alice@learnknow.io",
    "password": "wrongPassword"
  }
  ```
- **Expected Status:** `401 Unauthorized`
- **Expected Response:**
  ```json
  {
    "status": 401,
    "error": "Unauthorized",
    "message": "Invalid email or password"
  }
  ```

#### 6. NEGATIVE: Access Protected Endpoint Without Token (401 Unauthorized)
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/courses`
- *(Do NOT send any Authorization header)*
- **Expected Status:** `401 Unauthorized`
- **Expected Response:**
  ```json
  {
    "status": 401,
    "error": "Unauthorized",
    "message": "Missing or malformed Authorization header. Expected 'Bearer <token>'"
  }
  ```

#### 7. NEGATIVE: RBAC Violation - Student Creating Course (403 Forbidden)
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/courses`
- **Headers:**
  - `Authorization`: `Bearer {{studentToken}}`
  - `Content-Type`: `application/json`
- **Body:**
  ```json
  {
    "title": "Unauthorized Course Creation",
    "description": "Student should not have permission",
    "level": "BEGINNER"
  }
  ```
- **Expected Status:** `403 Forbidden`
- **Expected Response:**
  ```json
  {
    "status": 403,
    "error": "Forbidden",
    "message": "Access denied: Role 'STUDENT' does not have permission for this operation (requires ADMIN or INSTRUCTOR)"
  }
  ```

---

### Phase C: Course Management & Enrollment

#### 8. Admin Creates Course (RBAC Allowed)
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/courses`
- **Headers:**
  - `Authorization`: `Bearer {{adminToken}}`
  - `Content-Type`: `application/json`
- **Body:**
  ```json
  {
    "title": "Cloud-Native Java Microservices",
    "description": "Spring Cloud, Eureka, API Gateway, Docker",
    "level": "ADVANCED"
  }
  ```
- **Expected Status:** `201 Created`
- **Postman Tests Tab Script:**
  ```javascript
  const res = pm.response.json();
  pm.collectionVariables.set("courseId", res.id);
  ```

#### 9. Check Access Before Enrollment
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/access/check/1/{{courseId}}`
- **Headers:** `Authorization`: `Bearer {{studentToken}}`
- **Expected Status:** `200 OK`
- **Expected Response:**
  ```json
  {
    "userId": 1,
    "courseId": 1,
    "enrolled": false,
    "status": "NOT_ENROLLED"
  }
  ```

#### 10. Student Enrolls in Course
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/access/enroll`
- **Headers:**
  - `Authorization`: `Bearer {{studentToken}}`
  - `Content-Type`: `application/json`
- **Body:**
  ```json
  {
    "userId": 1,
    "courseId": 1
  }
  ```
- **Expected Status:** `201 Created`
- **Response:**
  ```json
  {
    "id": 1,
    "userId": 1,
    "courseId": 1,
    "status": "ACTIVE"
  }
  ```

#### 11. Check Access After Enrollment
- **Method:** `GET`
- **URL:** `{{baseUrl}}/api/access/check/1/{{courseId}}`
- **Headers:** `Authorization`: `Bearer {{studentToken}}`
- **Expected Status:** `200 OK`
- **Response:**
  ```json
  {
    "userId": 1,
    "courseId": 1,
    "enrolled": true,
    "status": "ACTIVE"
  }
  ```

---

### Phase D: Assignments & Code Submissions

#### 12. Admin Authors Assignment
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/assignments`
- **Headers:**
  - `Authorization`: `Bearer {{adminToken}}`
  - `Content-Type`: `application/json`
- **Body:**
  ```json
  {
    "courseId": 1,
    "title": "Lab 1: Global Gateway Filters",
    "description": "Implement reactive authentication with Spring Cloud Gateway",
    "dueDate": "2026-12-31T23:59:59"
  }
  ```
- **Expected Status:** `201 Created`
- **Postman Tests Tab Script:**
  ```javascript
  const res = pm.response.json();
  pm.collectionVariables.set("assignmentId", res.id);
  ```

#### 13. Student Submits Code
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/assignments/{{assignmentId}}/submit`
- **Headers:**
  - `Authorization`: `Bearer {{studentToken}}`
  - `Content-Type`: `application/json`
- **Body:**
  ```json
  {
    "userId": 1,
    "code": "public class Solution { public static void main(String[] args) { System.out.println(\"Review 2 Demo Passed!\"); } }"
  }
  ```
- **Expected Status:** `200 OK`
- **Response Status:** `"status": "SUBMITTED"`

---

### Phase E: Activity Tracking & Usage Progress

#### 14. Record Activity
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/usage/activity`
- **Headers:**
  - `Authorization`: `Bearer {{studentToken}}`
  - `Content-Type`: `application/json`
- **Body:**
  ```json
  {
    "userId": 1,
    "resourceId": 1,
    "action": "COMPLETED_MODULE"
  }
  ```
- **Expected Status:** `201 Created`

#### 15. Record Progress (75.0%)
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/usage/progress`
- **Headers:**
  - `Authorization`: `Bearer {{studentToken}}`
  - `Content-Type`: `application/json`
- **Body:**
  ```json
  {
    "userId": 1,
    "courseId": 1,
    "moduleId": 1,
    "completionPercent": 75.0
  }
  ```
- **Expected Status:** `200 OK`

#### 16. NEGATIVE: Record Invalid Progress (150%)
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/usage/progress`
- **Headers:**
  - `Authorization`: `Bearer {{studentToken}}`
  - `Content-Type`: `application/json`
- **Body:**
  ```json
  {
    "userId": 1,
    "courseId": 1,
    "moduleId": 1,
    "completionPercent": 150.0
  }
  ```
- **Expected Status:** `400 Bad Request`

---

### Phase F: AI Programming Assistant

#### 17. Ask AI: NullPointerException
- **Method:** `POST`
- **URL:** `{{baseUrl}}/api/ai/ask`
- **Headers:**
  - `Authorization`: `Bearer {{studentToken}}`
  - `Content-Type`: `application/json`
- **Body:**
  ```json
  {
    "userId": 1,
    "question": "What causes a NullPointerException in Java?"
  }
  ```
- **Expected Status:** `200 OK`
- **Response:**
  ```json
  {
    "id": 1,
    "question": "What causes a NullPointerException in Java?",
    "response": "A NullPointerException occurs when Java code tries to use an object reference that is null...",
    "timestamp": "2026-10-27T13:00:00Z"
  }
  ```
