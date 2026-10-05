# Sky Take-Out Backend Development Q&A

This document records the operations and problems that come up often during development, so you can keep developing and testing in IDEA.

## Project Location

```text
C:\Users\11619\IdeaProjects\sky-take-out
```

Open the `pom.xml` in the project root with IDEA and wait for the Maven dependencies to finish loading.

## Tech Environment

- Java 17 or later
- Spring Boot 3.3.5
- MyBatis-Plus
- MySQL 8
- Spring Security + JWT

## Database Initialization

1. Start the MySQL service in Windows Services.
2. Connect to MySQL in IDEA's Database panel.
3. Open `sql/schema.sql` in the project.
4. Run the entire script.
5. Refresh the database and confirm that the database `sky_take_out` exists with these tables:
   - `employee`
   - `category`
   - `dish`

If you see `Unknown database 'sky_take_out'`, the initialization script has not run successfully yet.

## Database Password Configuration

Database configuration files:

```text
src/main/resources/application.yml
src/main/resources/application-local.yml
```

The project enables the `local` profile by default. Change the password in `application-local.yml` to your own local MySQL root password. Do not commit the real password to Git or send it to anyone.

MySQL cannot show you the original root password. If you forget it, you can only use a saved configuration or go through the password reset procedure.

## Starting the Backend

1. Open `SkyTakeOutApplication.java` in IDEA.
2. Click the green triangle to the left of the class to run it.
3. Make sure the console finally prints:

```text
Started SkyTakeOutApplication
```

If you see `Process finished with exit code 1`, the application has exited and you cannot test any endpoints. Check the last `Caused by:` in the log first.

## Employee Login

On first startup, the application automatically creates a development account:

```text
Username: admin
Password: 123456
```

The login endpoint is a backend API, not a web page. You can run it from `test.http` in the project root:

```http
POST http://localhost:8080/api/admin/employee/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

After a successful login, copy the value of `data.token` from the response. Do not paste the whole JSON into the URL.

## Using the Token

For requests that require login, add this line below the URL:

```http
Authorization: Bearer your-token
```

There must be exactly one space between `Bearer` and the token. The token is valid for 24 hours by default; log in again after it expires.

## Category Management Endpoints

```http
### List categories
GET http://localhost:8080/api/admin/category/page?type=1
Authorization: Bearer your-token

### Add a category
POST http://localhost:8080/api/admin/category
Authorization: Bearer your-token
Content-Type: application/json

{
  "type": "1",
  "name": "Drinks",
  "sort": 4,
  "status": 1
}

### Update a category
PUT http://localhost:8080/api/admin/category
Authorization: Bearer your-token
Content-Type: application/json

{
  "id": 1,
  "type": "1",
  "name": "Staples",
  "sort": 1,
  "status": 1
}

### Enable or disable a category
PATCH http://localhost:8080/api/admin/category/1/status/0
Authorization: Bearer your-token

### Delete a category
DELETE http://localhost:8080/api/admin/category/1
Authorization: Bearer your-token
```

## Dish Management Endpoints

```http
### List dishes
GET http://localhost:8080/api/admin/dish/page
Authorization: Bearer your-token

### Add a dish
POST http://localhost:8080/api/admin/dish
Authorization: Bearer your-token
Content-Type: application/json

{
  "name": "Kung Pao Chicken",
  "categoryId": 1,
  "price": 28.00,
  "image": "",
  "description": "Classic Sichuan-style dish"
}

### Enable or disable a dish
PATCH http://localhost:8080/api/admin/dish/1/status/0
Authorization: Bearer your-token
```

`categoryId` and the `id` in the dish URL must use the actual IDs in your database.

## Common Errors

### Connection refused

Nothing is listening on port 8080, so the backend is not running. Run `SkyTakeOutApplication` again and confirm the application does not exit.

### Public Key Retrieval is not allowed

The project's connection configuration already includes `allowPublicKeyRetrieval=true`. After changing the configuration, rerun the application.

### Access denied for user 'root'

The database password is wrong. Check the password in `application-local.yml` and confirm that the MySQL service is running.

### 401 Unauthorized

The request has no token, the token format is wrong, or the token has expired. Log in again and send the `Authorization: Bearer token` header.

## Current Development Progress

- Employee login and JWT authentication: done
- Category CRUD, enable/disable: done
- Basic dish CRUD, enable/disable: done
- Vue frontend pages: to do
- Set Meals, cart, orders, Redis: to do
