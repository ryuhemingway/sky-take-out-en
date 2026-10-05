# Sky Take-Out Project Summary and Interview Review

## 1. Project Positioning

Sky Take-Out is a food delivery ordering system with decoupled frontend and backend. It has three parts: an admin dashboard, a customer web UI, and a backend service.

The project addresses two kinds of business problems:

- Admins maintain Shop, category, dish, set meal, order, coupon, and rider delivery information.
- Users browse dishes, add them to the cart, submit orders, use coupons, check delivery status, and receive real-time notifications.

The project already covers the complete ordering flow:

```text
User login -> browse menu -> add to cart -> choose address -> use coupon
       -> submit order -> admin accepts the order -> assign a rider -> delivery -> complete order
       -> user views order history and real-time delivery notifications
```

## 2. Overall Architecture

### 2.1 Technical Architecture

```text
Admin dashboard Vue 3 + Vite  ─┐
                               ├─ HTTP/JSON ─ Spring Boot REST API ─ MySQL
Customer web UI Vue 3 + Vite  ─┘                         ├─ Redis/cache
                                                         └─ STOMP WebSocket
```

### 2.2 Tech Stack

Backend:

- Java 17
- Spring Boot 3.3.5
- Spring MVC
- Spring Security
- JWT 0.12.6
- MyBatis-Plus 3.5.9
- MySQL 8
- Spring Data Redis
- Spring WebSocket + STOMP
- Maven
- Lombok

Frontend:

- Vue 3 Composition API
- Vite
- Vue Router
- lucide-vue-next icon library
- @stomp/stompjs
- Fetch API

### 2.3 Backend Layers

```text
Controller  Receives requests, validates parameters, returns a unified response
Service     Orchestrates business flows, controls transactions, handles state transitions
Mapper      MyBatis-Plus data access
Entity      Database table mapping
Config      Security, cache, WebSocket, and initialization configuration
Security    JWT generation, parsing, and request authentication
```

The unified response structure is:

```json
{
  "code": 1,
  "msg": "success",
  "data": {}
}
```

## 3. Database Design

Core tables:

| Table | Purpose |
|---|---|
| `employee` | Admin accounts |
| `user` | User accounts |
| `category` | Dish and set meal categories |
| `dish` | Dishes |
| `setmeal` | Set Meals |
| `setmeal_dish` | Links between set meals and dishes |
| `shopping_cart` | User cart |
| `address_book` | User delivery addresses |
| `orders` | Order header table |
| `order_detail` | Order line items |
| `coupon` | Coupon templates and stock |
| `user_coupon` | Records of coupons a user has claimed and used |
| `rider` | Riders |
| `delivery` | Order delivery records |

The `orders` table stores an order snapshot, including the recipient, phone number, address, original price, discount amount, amount paid, and remarks. This way, even if a user later edits an address, historical orders keep the information from the time of ordering.

Coupons use two tables:

- `coupon` stores the coupon rules, validity period, and remaining stock.
- `user_coupon` stores per-user state such as whether the coupon is claimed, whether it is used, and which order it belongs to.

Order history deletion uses a soft-delete column, `orders.user_deleted`. The record is hidden in the customer web UI, while the admin dashboard can still keep statistics and audit data.

## 4. Development Phase Retrospective

### Phase 1: Project Initialization and Database Setup

What was built:

- Created the Spring Boot project and Maven dependencies.
- Configured Java 17, MySQL, Redis, and JWT parameters.
- Wrote `sql/schema.sql` to initialize the database and seed data.
- Configured `application-local.yml` to hold the local database password.
- Used `DataInitializer` to create the development admin account.

Retrospective:

The most important thing in this phase was to pin down the runtime environment first. Many later API problems were not code errors but MySQL not running, the database not initialized, a wrong password, or the profile not being enabled. The project keeps the local password in `application-local.yml` so that the real password stays out of the shared configuration and Git.

### Phase 2: Admin Login and Authentication

What was built:

- Admin login endpoint: `POST /api/admin/employee/login`.
- User login endpoint: `POST /api/user/login`.
- `JwtService` generates and parses JWTs.
- `JwtAuthFilter` extends `OncePerRequestFilter` and reads the token from `Authorization: Bearer token`.
- Admins and regular users are told apart by whether the JWT contains `employeeId` or `userId`.
- `SecurityConfig` protects endpoints with `hasRole("ADMIN")` and `hasRole("USER")`.

Key flow:

```text
Login succeeds -> server issues a JWT -> frontend stores the token
Request an endpoint -> send the Bearer token -> JWT filter parses it
        -> sets userId / admin identity -> Spring Security allows or rejects the request
```

Retrospective:

JWT is stateless authentication, so the server does not need to store a Session. Three points to watch:

1. When a token expires, the API returns 401, and the frontend clears the local token and redirects to login.
2. Parsing a token does not mean the caller has permission; role-based authorization in Spring Security is still required.
3. When a user queries orders, addresses, or the cart, the backend must use the `userId` from the request context to look up that user's own data, and must never trust a user ID sent by the frontend.

### Phase 3: Category, Dish, and Set Meal Management

What was built:

- Category CRUD, enable/disable, and sorting.
- Dish CRUD, query by category, and enable/disable.
- Set Meal CRUD and maintenance of the set meal and dish links.
- The admin dashboard pages provide tables, search, modal editing, and status toggles.
- The customer web UI shows only enabled categories, dishes, and set meals.

Retrospective:

The admin dashboard and the customer web UI use different query conditions: the admin dashboard needs to see every status, while the customer web UI only needs items that are on sale. When designing the endpoints, the meaning of `status` should be explicit, so that disabled dishes are never shown to users by mistake.

### Phase 4: Customer Menu, Cart, and Address Book

What was built:

- The customer web UI browses dishes by category.
- Dish images are mapped by dish ID, with a fallback image when an image is missing.
- Dish detail modal, floating cart, and quantity buttons.
- Delivery addresses can be added, queried, edited, and deleted, with a default address.
- When not logged in, the frontend shows a login modal; after login it loads the menu, cart, addresses, and orders.

Retrospective:

The cart is not just frontend state; the database is the source of truth. After a frontend refresh, a device change, or a new login, the cart must be reloaded from the backend. The item name, price, and image in the cart are a snapshot taken when the item was added, so later dish edits do not make an existing cart display incorrectly.

### Phase 5: Order Submission, Simulated Payment, and the State Machine

Order status design:

| Status | Meaning |
|---:|---|
| 1 | Pending Payment |
| 2 | Awaiting Acceptance |
| 3 | Accepted |
| 4 | Out for Delivery |
| 5 | Completed |
| 6 | Cancelled |

Core flow when submitting an order:

1. Check that the address belongs to the current user.
2. Query the current user's cart.
3. Calculate the original total.
4. Validate the coupon and calculate the discount.
5. Create the order header record.
6. Save the order line items.
7. Clear the cart.
8. Notify the admin dashboard of the new order.

`OrderServiceImpl.submit` uses `@Transactional`, which guarantees that the order header, the line items, the cart cleanup, and the coupon redemption either all succeed or are all rolled back.

The payment endpoint is a simulated payment for the development environment: it moves the order from Pending Payment to Awaiting Acceptance and records the payment time.

Retrospective:

The order is the core aggregate of the whole system. State changes cannot be made only in the frontend; the backend must validate them against the current status. For example, only status 2 can be accepted, only status 3 can move to delivery, and only status 4 can be completed. The endpoints use `expectedStatus` to prevent duplicate operations and illegal jumps.

### Phase 6: Coupon System

What was built:

- Users query valid coupons.
- Users claim coupons.
- Admins create, enable, and disable coupons.
- On ordering, the coupon code, validity period, minimum spend, claim state, and usage state are validated.
- `CouponMapper.decreaseStock` uses a conditional update to decrement stock atomically:

```sql
UPDATE coupon
SET remaining_stock = remaining_stock - 1
WHERE id = ? AND remaining_stock > 0
```

Retrospective:

Coupons are the most prone to stock overselling. Reading the stock first and then updating it is not concurrency-safe, because two requests may read the same stock value at the same time. So you must use a conditional atomic SQL statement and check that the number of affected rows is 1. Claiming and redeeming also need to be told apart: claiming creates a `user_coupon` row, and a successful order marks it as used.

### Phase 7: Redis and Shop Status

What was built:

- Redis stores the shop open/closed status.
- `ShopStatusService` reads and sets `sky:shop:status`.
- When Redis is unavailable, an in-memory `AtomicInteger` serves as a fallback, so the shop endpoints do not become completely unusable in development just because Redis is not running.
- The Cache configuration sets a default expiry time and a cache exception handler.
- A scheduled task checks unpaid orders every 5 minutes and automatically cancels those older than 30 minutes.

Retrospective:

Redis is not a hard dependency for every feature. Shop status is simple state that suits caching, but production should also consider Redis persistence, clustering, high availability, and consistency of status changes. The order timeout task must be idempotent, so that running it repeatedly does not charge twice or create duplicate data.

### Phase 8: Rider Delivery System

What was built:

- Riders can be added, enabled, and disabled.
- Admins assign an available rider to an accepted order.
- The rider confirms pickup, and the delivery record changes to Out for Delivery.
- The rider confirms delivery, the order becomes Completed, and the rider becomes idle again.
- Users query the delivery information for an order, including the rider's name, phone number, and delivery times.
- The admin dashboard provides a rider delivery page.

Delivery status:

```text
0 Pending Assignment -> 1 Rider Assigned -> 2 On the Way -> 3 Delivered
```

Retrospective:

The order status and the delivery status are two related but separate state machines. The order represents the fulfillment stage of the transaction, and the delivery record represents the execution stage of the rider. You cannot change only the order status without updating the delivery record, or the order status the user sees will be inconsistent with the rider status.

### Phase 9: WebSocket Real-Time Notifications

What was built:

- Spring WebSocket configures a STOMP broker.
- Connection endpoint: `/ws`.
- The admin dashboard subscribes to `/topic/admin`.
- The customer web UI subscribes to `/topic/user/{userId}`.
- `NotificationService` sends all notifications.
- The JWT is validated on WebSocket CONNECT.
- On SUBSCRIBE, admins are restricted to the admin topic, and users to their own topic.
- The admin dashboard shows online status, an unread count, a message list, and a toast.
- The customer web UI shows connection status, delivery notifications, and message history.

Business notification events:

- `NEW_ORDER`: a user submitted an order.
- `ORDER_PAID`: a user paid for an order.
- `REMINDER`: a user sent an order reminder.
- `RIDER_ASSIGNED`: a rider was assigned.
- `DELIVERY_PICKED`: the rider picked up the order.
- `DELIVERY_COMPLETED`: the order was delivered.

Retrospective:

WebSocket is suited to sending state-change events, not to replacing the database. After receiving a notification, the frontend still calls the order query endpoint to refresh the data, so even if a message is lost, the page still gets the correct state when reopened. In real verification, a temporary order was used to confirm that the admin dashboard showed the unread red dot, a message list entry, and a refreshed order page.

### Phase 10: Order History Query and Deletion

What was built:

- Users query orders by status.
- Users search orders by fuzzy match on the order number.
- Only Completed or Cancelled orders can be deleted.
- Soft deletion through `user_deleted`; orders are never physically deleted.
- The admin dashboard can still see the full set of orders and statistics.

Retrospective:

When a user deletes an order from their history, the order table should not be hit with a plain `DELETE`, because order line items, coupon redemption, delivery records, and operational statistics may all depend on it. Soft deletion satisfies the user experience while keeping audit and statistics capabilities.

### Phase 11: Shop Closed and Checkout Failure

What was built:

- The admin dashboard toggles the shop between open and closed.
- The customer web UI shows the shop status in real time.
- If the checkout page already knows the shop is closed, it immediately shows an "Order checkout failed" dialog.
- If the shop closes after the user has opened the checkout page, submitting the order catches the backend error and shows a dialog.
- The backend validates the shop status again at the order entry point, because it cannot trust only the frontend button state.

Retrospective:

Frontend validation only improves the experience; backend validation is the final constraint. Any decision involving stock, amounts, shop status, and permissions must be executed again on the server.

## 5. Problems Encountered and How They Were Solved

### 1. Opening an endpoint URL shows something odd

An API is not a web page. The browser address bar only shows JSON or an error response and cannot render an admin interface the way a web page can. Call the endpoints with `test.http`, Postman, or the frontend pages, and include the JWT.

### 2. 401 Unauthorized

The usual causes are a missing token, an expired token, a wrong Bearer format, or mixing up user and admin tokens. The fix is to log in again and use:

```text
Authorization: Bearer your-token
```

### 3. MySQL connection failure

Common causes include the MySQL service not running, the database not existing, a wrong password, and `allowPublicKeyRetrieval` not being configured. Diagnose it by running `schema.sql`, checking the local configuration, and reading the last `Caused by`.

### 4. Redis not running

A failed Redis connection affects cache operations. The shop status service has an in-memory fallback, and the cache exception handler ignores cache read/write errors, so the core endpoints keep working. Production should not rely on the fallback for long; deploy Redis and set up monitoring.

### 5. WebSocket had no visible effect

If the backend only sends messages and there is no visible feedback in the frontend, users will think the feature is not implemented. Later, a connection status, a bell, an unread red dot, a message center, a toast, and automatic refresh were added, and an end-to-end check was done with a temporary order.

### 6. Coupon stock overselling

A plain "read the stock, then update it" has a concurrency problem. It was changed to an atomic update with the `remaining_stock > 0` condition, plus a check of the affected row count.

### 7. Order and delivery status out of sync

The old delivery buttons on the order page could change the order status directly and bypass the delivery record. Later, the old entry points on the order page were also wired into the delivery state machine, so `delivery`, `rider`, and `orders` are all updated together.

### 8. Customer web UI images not matching Dishes

Some dishes in the database have no image URL. The frontend added a unique image mapping by dish ID and a default fallback image, and the admin dashboard also supports maintaining the dish image field.

### 9. Garbled Chinese text in the database

The usual cause is a mismatch between the connection charset, the file encoding, and the terminal display encoding. Database tables all use `utf8mb4`, the connection sets `useUnicode=true&characterEncoding=utf-8`, and files are saved as UTF-8.

## 6. Interview Project Introduction Templates

### 1-Minute Version

I built a food delivery ordering system with decoupled frontend and backend, including an admin dashboard and a customer web UI. The backend uses Java 17, Spring Boot, MyBatis-Plus, MySQL, Redis, Spring Security, and JWT, and the frontend uses Vue 3 and Vite. The system implements category, dish, and set meal management, plus the user cart, address book, orders, coupons, and rider delivery, and it uses WebSocket for real-time notifications of new orders, order reminders, and delivery status changes.

I was mainly responsible for backend business design, database table design, JWT authentication, order state transitions, coupon stock deduction, the rider delivery state machine, and the admin dashboard and customer web UI pages. The representative hard problems in the project were coupon stock concurrency control, consistency between order and delivery status, WebSocket subscription permissions, and double validation on the frontend and backend when the shop is closed.

### 3-Minute Version Structure

1. Start with the business background: serving user ordering and Shop fulfillment management.
2. Then the architecture: two Vue frontends, one Spring Boot backend, MySQL, Redis, and WebSocket.
3. Then the core flow: login, menu, cart, ordering, accepting the order, delivery, completion.
4. Go deep on two hard problems: concurrent coupon deduction and WebSocket real-time notifications.
5. Finish with testing and results: endpoints, Maven, frontend builds, and end-to-end notifications were all verified.

## 7. Interview Q&A

### Spring Boot

**Q: What is Spring Boot auto-configuration?**

A: Spring Boot automatically creates Beans based on the dependencies on the classpath, the configuration files, and conditional annotations, which reduces XML configuration. For example, after adding the WebSocket starter, `@EnableWebSocketMessageBroker` plus a configuration class is enough to enable the message broker.

**Q: When does `@Transactional` take effect?**

A: Spring implements transactions with proxies, which usually requires the method to be public and to be called through the proxy object. In this project, order submission, coupon redemption, saving order line items, and cart cleanup run in the same transaction, and the whole thing rolls back when a runtime exception occurs.

### Spring Security and JWT

**Q: Why is JWT a good fit for a decoupled frontend and backend?**

A: A JWT carries the identity information, and the server can identify the user just by verifying the signature, without relying on a shared Session, which suits multiple frontends or multiple service instances. But you must set an expiry time, protect the secret key, avoid putting sensitive information in the payload, and handle token invalidation.

**Q: What is the difference between authentication and authorization?**

A: Authentication confirms "who you are", which JWT handles in this project. Authorization confirms "what you can access", which in this project is done by limiting endpoint paths and operations with `ROLE_ADMIN` and `ROLE_USER`.

### MyBatis-Plus

**Q: What are the advantages of MyBatis-Plus?**

A: Built on MyBatis, it provides generic CRUD, condition builders, pagination, and entity mapping, which cuts down on repetitive SQL. In this project, `LambdaQueryWrapper` combines the conditions for user ID, status, time, and order number, while a native `@Update` SQL statement is kept for atomic coupon stock deduction.

**Q: Why can't you trust the userId sent by the frontend?**

A: Client parameters can be tampered with. The backend should get the current user's identity from the JWT or the request context and then add the data permission conditions itself, to prevent unauthorized access to other users' orders and addresses.

### MySQL

**Q: Why does an order store a snapshot of the delivery information?**

A: An order is a historical fact and should not change when the user edits an address. At ordering time, the recipient, phone number, and detailed address are copied into the order table so that the order stays traceable.

**Q: How do you prevent coupon stock from being oversold?**

A: Use a conditional atomic update and check the affected row count: decrement by one only when `remaining_stock > 0`. If needed, add transactions, unique constraints, and idempotency checks.

**Q: How do you choose between soft deletion and physical deletion?**

A: Orders, payments, delivery, and statistics data generally use soft deletion, while temporary cart data can be physically deleted. In this project, order history uses `user_deleted`, to avoid breaking operational data.

### Redis

**Q: What does Redis do in this project?**

A: It mainly stores the shop open/closed status and provides caching support. The shop status is read from and written to Redis, and the cache has a TTL. There is an in-memory fallback when Redis is temporarily unavailable, but production should still have high availability and monitoring.

**Q: How do you handle cache penetration, breakdown, and avalanche?**

A: For penetration, use parameter validation, caching null values, or a Bloom filter. For breakdown, use a mutex lock or logical expiration. For avalanche, use randomized TTLs, staged warm-up, and rate limiting with degradation. The current project is small, and the focus was on TTL and degrading gracefully on cache errors.

### WebSocket/STOMP

**Q: What is the difference between WebSocket and HTTP?**

A: With HTTP the client usually initiates each request, while after a WebSocket long-lived connection is established the server can push messages proactively, which suits new orders, order reminders, and delivery status changes. The project uses the STOMP protocol to manage connections, subscriptions, and message destinations.

**Q: How do you make sure users only receive their own messages?**

A: On the connection handshake, the JWT is parsed to establish a Principal, and the destination is validated on subscription. Admins can only subscribe to `/topic/admin`, and users can only subscribe to `/topic/user/{their own userId}`.

**Q: What if a WebSocket message is lost?**

A: WebSocket is only a real-time alert, and the database is the final source of truth. After receiving an event, the frontend queries the orders again, and it also reloads the orders when the page is reopened. In production you can also add message persistence, reconnect compensation, and an offline unread-message table.

### Vue Frontend

**Q: Why do you need two frontends, one for the admin dashboard and one for the customer web UI?**

A: Their roles, page structure, and permissions are completely different. Splitting them lets them be deployed, built, and evolved independently: the admin dashboard is for operations staff, and the customer web UI is for users who order food.

**Q: Why does the frontend handle the shop being closed when the backend validates it too?**

A: Frontend validation is for immediate feedback and a better experience, and backend validation is for security and consistency. A user may stay on the checkout page for a long time, or bypass the page and call the endpoint directly, so the final validation must happen in the backend.

## 8. Current Limits of the Project and Future Improvements

In an interview, be honest that this is a complete development-environment MVP, not a production-grade commercial system. Possible next steps:

- Integrate a real third-party payment provider and verify payment callback signatures.
- Use Redis distributed locks or a message queue to further improve high-concurrency stock scenarios.
- Add message persistence, offline compensation, and cluster broadcast to WebSocket.
- Add unit tests, integration tests, automated API tests, and coverage reporting.
- Add log tracing, unified exception handling, operation auditing, and monitoring alerts.
- Upload images to object storage instead of maintaining a large ID mapping in the frontend.
- Use Docker, Nginx, and CI/CD for deployment.
- Add stricter state transition tables and idempotency keys for the order, coupon, and delivery state machines.

## 9. Final Retrospective

What is most worth emphasizing in an interview is not the number of pages but the end-to-end business loop and the handling of consistency:

1. JWT and Spring Security isolate the two identity types, admin and user.
2. Transactions keep the order header, line items, cart, and coupon redemption consistent.
3. Atomic SQL solves concurrent coupon stock deduction.
4. Splitting the order status from the delivery status avoids muddled fulfillment information.
5. WebSocket pushes business events, and HTTP queries then guarantee the final data is correct.
6. Soft deletion of order history balances user experience, operational statistics, and audit needs.
7. Double validation of the shop status on the frontend and backend ensures no more orders can be placed after closing.

When presenting in an interview, follow the order "business background -> technical architecture -> core flow -> hard problem solutions -> verification results -> future improvements", keep it within 3 minutes, and then go into details as the interviewer asks follow-up questions.
