# EventHub

EventHub is a responsive event discovery and booking application. It includes a static HTML/CSS/JavaScript frontend and a Java 17 / Spring Boot REST API backed by MySQL. Accounts use BCrypt password hashing and expiring JWTs. Ticket inventory is allocated while holding a database row lock, and prices are calculated by the server.

## Project structure

```text
backend/
  pom.xml
  .env.example
  src/main/java/com/eventhub/
    dto/              Validated request and response records
    model/            JPA entities and role/status enums
    repository/       Spring Data JPA repositories
    security/         JWT service/filter and Spring Security/CORS
    service/          Auth, event, booking and admin operations
    web/              REST controllers and API exception handler
  src/main/resources/
    application.properties
    schema.sql        Reference relational schema
    demo-data.sql     Example events for a registered organizer
  src/test/java/      Service and API integration tests
frontend/
  *.html              Home, events, event details, auth, bookings, dashboards, profile, 404
  assets/app.js       Shared fetch client and page interactions
  assets/styles.css   Responsive shared design system
postman/
  EventHub.postman_collection.json
```

## Prerequisites

- Java 17 or newer
- Apache Maven 3.8+
- MySQL 8+
- Node.js 18+ (to run the included dependency-free frontend server)
- A modern browser

## Run locally

### One-command startup on Windows

Run `start-website.cmd` from File Explorer or a terminal. It starts the frontend and API in separate terminal windows, waits until both respond, then opens `http://localhost:5500`. The API uses the persistent local H2 demo database, so MySQL setup is not required for this workflow. Demo events are based in India, and ticket and booking prices are shown in Indian rupees (INR). Run the command again to reopen the site; already-running EventHub servers are reused. Keep the server windows open while using the site. In VS Code, choose **Launch EventHub in Chrome** to start the same services before opening the browser.

### Ticket bookings

Booking a ticket immediately confirms it and reserves the available inventory. The ticket appears as **BOOKED** under My bookings. No payment provider or payment setup is needed. Customers can view and print confirmed ticket references from My bookings. Cancelling an eligible booking returns its tickets to inventory.

### Manual startup

1. Install Java, Maven, and MySQL. Start MySQL and create the database:

   ```sql
   CREATE DATABASE eventhub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

   Hibernate creates and updates the tables from the JPA entities on startup. `backend/src/main/resources/schema.sql` documents the equivalent tables and foreign keys.

2. Configure the backend. The defaults target `localhost:3306`, user `root`, and an empty password. Set environment variables in the terminal where the backend will run (replace the sample values):

   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/eventhub?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "your-local-mysql-password"
   $env:JWT_SECRET = "use-a-unique-random-secret-at-least-32-characters-long"
   $env:CORS_ALLOWED_ORIGIN = "http://localhost:5500"
   ```

   `backend/.env.example` lists all supported settings. Environment variables are read directly by Spring; Maven does not automatically load a `.env` file. Never use the sample JWT key outside local development.

3. Start the API from the project root:

   ```powershell
   mvn -f backend\pom.xml spring-boot:run
   ```

   The API listens at `http://localhost:8081`. Its CORS allowlist defaults to `http://localhost:5500`.

4. In another terminal, serve the frontend:

   ```powershell
   npm --prefix frontend start
   ```

   Open `http://localhost:5500`. The browser frontend calls the API at `http://localhost:8081/api`; update the `API` constant near the top of `frontend/assets/app.js` if the API runs elsewhere. The static server uses only Node's built-in modules; it installs no packages.

### Quick demo without MySQL credentials

The default profile uses MySQL. To run a local demo with a persistent embedded H2 database instead, start the API with the `demo` profile:

```powershell
$env:SPRING_PROFILES_ACTIVE = "demo"
mvn -f backend\pom.xml spring-boot:run
```

The demo profile creates sample events and demo accounts once in `backend\data\eventhub.mv.db`. Sign in as `organizer@eventhub.local` / `Organizer123!` to create events, or `admin@eventhub.local` / `Admin12345!` to use the admin console. Set `DEMO_ORGANIZER_PASSWORD` and `DEMO_ADMIN_PASSWORD` before first startup to change these local demo credentials. Do not enable the demo profile on a public deployment.

## First-run demo setup

1. Register a normal account at `/register.html`.
2. If you want to manage events, promote an account using MySQL Workbench (replace the email):

   ```sql
   UPDATE users SET role = 'ORGANIZER' WHERE email = 'organizer@eventhub.local';
   ```

   For an administrator, set `role = 'ADMIN'` instead. Public registration intentionally always creates `USER` accounts; a client cannot assign privileged roles.
3. Log out and back in, then create an event in the organizer dashboard. Or register an organizer as `organizer@eventhub.local`, promote it, and run `backend/src/main/resources/demo-data.sql` to load three sample events. The SQL expects that account to exist.
4. Browse events and book tickets from the event details page. Bookings for both free and paid events are confirmed immediately.

## API overview

All JSON request fields match the frontend and Postman collection. Authenticated requests send `Authorization: Bearer <token>`.

| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register `{fullName,email,password}`; new accounts are `USER` |
| POST | `/api/auth/login` | Public | Login `{email,password}` and receive `{token,user}` |
| GET / PUT | `/api/auth/me` | Signed in | Read or update `{fullName}` |
| PUT | `/api/auth/me/password` | Signed in | Change `{currentPassword,newPassword}` |
| GET | `/api/events?search=&category=` | Public | Browse/filter active events |
| GET | `/api/events/{id}` | Public | Event details |
| POST / PUT / DELETE | `/api/events[/{id}]` | Organizer/Admin | Create, update, or delete an event |
| POST | `/api/bookings` | Signed in | Book and confirm `{eventId,ticketQuantity}` immediately |
| GET | `/api/bookings/my` | Signed in | Own bookings |
| GET | `/api/bookings/{id}` | Owner/Admin | Booking details |
| PATCH | `/api/bookings/{id}/cancel` | Owner/Admin | Cancel an eligible booking |
| GET | `/api/organizer/events` | Organizer/Admin | Organizer's events |
| GET | `/api/organizer/events/{id}/attendees` | Owner/Admin | Attendees for an owned event |
| GET | `/api/organizer/dashboard` | Organizer/Admin | Organizer statistics |
| GET | `/api/admin/dashboard` | Admin | Platform counts |
| GET | `/api/admin/users` | Admin | Users and roles |
| PATCH | `/api/admin/users/{id}/role` | Admin | Set `{role:"USER"|"ORGANIZER"|"ADMIN"}` |
| GET | `/api/admin/events` | Admin | All events, including cancelled ones |
| PATCH | `/api/admin/events/{id}/cancel` | Admin | Cancel an event and stop new bookings |
| GET | `/api/admin/bookings` | Admin | All bookings |

Invalid input returns a JSON error with HTTP 400; missing records return 404; conflicting inventory, duplicate accounts, and invalid cancellations return 409; unauthorized access returns 401/403. The admin console retains booking history and cancels an event rather than deleting it. An event with booking history cannot be physically deleted.

## Database relationships and concurrency

- `users` has a unique email and owns events and bookings.
- `events.organizer_id` references `users.id`.
- `bookings.user_id` and `bookings.event_id` reference their user and event.
- A booking request uses `READ_COMMITTED` and locks its event row in a transaction (`PESSIMISTIC_WRITE`), checks inventory and a 20-ticket-per-attendee/event limit, calculates the ticket total from the persisted price, reserves inventory, and creates a confirmed booking immediately.
- Booking cancellation and event cancellation lock the event before changing booking state; customer cancellations then lock the booking before restoring ticket inventory.

## Tests and API collection

Run the backend unit and integration tests from the project root:

```powershell
mvn -f backend\pom.xml test
```

The integration tests use an isolated in-memory H2 database. They cover registration, JWT-authenticated profile access, duplicate registration, unauthenticated booking rejection, persistent ticket booking, correct totals, and user-scoped booking lists. Booking service tests also cover inventory decrement, past-event rejection, and insufficient-capacity rejection.

Import `postman/EventHub.postman_collection.json` into Postman. Run Register, then Login (which saves the token to the collection variable), then browse events and book. Organizer and administrator requests require a promoted account and a fresh login. Change the collection's `eventId` and `bookingId` variables to IDs returned by your local API.

## GitHub

Create an empty GitHub repository, then from this project folder run:

```powershell
git init
git add .
git commit -m "Build EventHub event management platform"
git branch -M main
git remote add origin https://github.com/YOUR-USER/YOUR-REPOSITORY.git
git push -u origin main
```

Do not add local passwords, production keys, or `.env` files to Git. `.gitignore` excludes those files and common build artifacts.
