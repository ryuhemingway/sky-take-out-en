# Sky Take-Out

A food delivery ordering system with decoupled frontend and backend, consisting of an admin dashboard, a customer web UI, and a Spring Boot backend.

For the full project design, development retrospective, analysis of hard problems, and interview Q&A, see: [PROJECT_REVIEW.md](PROJECT_REVIEW.md).

## Tech Stack

- Backend: Java 17, Spring Boot 3.3.5, MyBatis-Plus, MySQL 8, Spring Security, JWT, Redis, STOMP WebSocket
- Frontend: Vue 3, Vite, Vue Router, @stomp/stompjs, lucide-vue-next

## Implemented Features

- Admin and user login, with JWT-based permission isolation
- Category, dish, and set meal management
- Customer menu, cart, and address book
- Placing orders, simulated payment, cancellation, order reminders, and automatic cancellation on timeout
- Coupon claiming, redemption, and stock management
- Rider management, order assignment, pickup, and delivery
- Shop open/closed control
- Customer order queries and soft deletion
- Real-time WebSocket notifications for the admin dashboard and customer web UI

## Running Locally

1. Run `sql/schema.sql` on MySQL 8. If you already have a database from the Chinese version, run `sql/migrate-to-english.sql` on it once instead.
2. Set your local database password in `src/main/resources/application-local.yml`.
3. Start Redis. If Redis is unavailable in development, the shop status falls back to in-memory storage, but you should start Redis normally.
4. Run the backend from the project root:

```powershell
D:\mysql\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

5. Start the two frontends separately:

```powershell
cd frontend
npm install
npm run dev
```

```powershell
cd user-frontend
npm install
npm run dev
```

Default addresses:

- Backend: `http://localhost:8080`
- Admin dashboard: `http://127.0.0.1:5173`
- Customer web UI: `http://127.0.0.1:5174`

Development admin account: `admin / 123456`.

## Verification

- HTTP API tests: `test.http`
- Backend build: `mvn test`
- Admin dashboard build: run `npm run build` in the `frontend` directory
- Customer web UI build: run `npm run build` in the `user-frontend` directory
