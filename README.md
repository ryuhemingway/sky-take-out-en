# Sky Take-Out

A food delivery ordering system with decoupled frontend and backend, consisting of an admin dashboard, a customer web UI, and a Spring Boot backend.

For the full project design, development retrospective, analysis of hard problems, and interview Q&A, see: [PROJECT_REVIEW.md](PROJECT_REVIEW.md).

## Tech Stack

- Backend: Java 17, Spring Boot 3.3.5, MyBatis-Plus, MySQL 8 (Google Cloud SQL), Spring Security, JWT, STOMP WebSocket
- Frontend: Vue 3, Vite, Vue Router, @stomp/stompjs, lucide-vue-next

## Implemented Features

- Two roles: admins (employees) and customers, each with password login (BCrypt) and JWT-based permission isolation
- Customer registration and profile: edit name and phone, change password, delete account
- Admin management (full CRUD) of customers, employees, categories, dishes, set meals, coupons and riders
- Customer menu, cart, and address book
- Placing orders, simulated payment, cancellation, order reminders, and automatic cancellation on timeout
- Coupon claiming, redemption, and stock management
- Rider assignment, pickup, and delivery
- Shop open/closed control (stored in MySQL)
- Customer order queries and soft deletion
- Real-time WebSocket notifications for the admin dashboard and customer web UI

## Database on Google Cloud SQL

The project database runs on MySQL in Google Cloud SQL. The web app can run on your own computer.

1. In the Google Cloud console, open **SQL > Create instance > MySQL**. Choose MySQL 8.0, the cheapest edition/preset, and set a root password.
2. Open the instance's **Connections > Networking** tab, keep **Public IP** on, and add your own IP address (`x.x.x.x/32`) as an authorized network. Each teammate adds their own IP.
3. Copy the instance's public IP and load the schema from your computer:

```bash
mysql -h <PUBLIC_IP> -u root -p < sql/schema.sql
```

4. Point the backend at Cloud SQL with environment variables (never commit the password):

```bash
export DB_URL="jdbc:mysql://<PUBLIC_IP>:3306/sky_take_out?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&sslMode=REQUIRED"
export DB_USERNAME=root
export DB_PASSWORD='your-root-password'
```

If the connection is refused, check that your current IP is still in the authorized networks. If it fails with an SSL error, try `sslMode=PREFERRED`.

## Running Locally

1. Set up the database (Cloud SQL above, or any MySQL 8 for development) and set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`. Without them the backend uses a local MySQL at `127.0.0.1:3306`; `src/main/resources/application-local.yml` also works for local overrides.
2. Run the backend from the project root with JDK 17 (newer JDKs break Lombok in this project):

```bash
mvn spring-boot:run
```

3. Start the two frontends separately:

```bash
cd frontend
npm install
npm run dev
```

```bash
cd user-frontend
npm install
npm run dev
```

Default addresses:

- Backend: `http://localhost:8080`
- Admin dashboard: `http://localhost:5173`
- Customer web UI: `http://localhost:5174`

Demo accounts (created automatically on first start):

- Admin: `admin / 123456`
- Customer: `dev-user-001 / 123456`

## Verification

- HTTP API tests: `test.http`
- Backend build: `mvn test`
- Admin dashboard build: run `npm run build` in the `frontend` directory
- Customer web UI build: run `npm run build` in the `user-frontend` directory
