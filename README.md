#🚖 Cab Booking Application

A full-stack ride-hailing platform (Uber/Ola style) built with **Java, Spring Boot, PostgreSQL and React**.
Riders book rides on a map, drivers accept them, both see live updates over WebSockets, and payments are handled with Stripe.

## Features

**Rider**
- Register / login (JWT, role-based access)
- Pick pickup and drop by **searching a place** or **clicking the map** (Leaflet + OpenStreetMap)
- Instant distance and fare estimate (Haversine formula)
- Book, cancel, and track a ride in real time (status, driver location, driver arrival)
- SOS alert during a ride
- Pay online with Stripe (test mode), download a **PDF invoice**
- Rate the driver (1-5 stars + comment), view ride history
  **Driver**
- Go online / offline and share location (search, map click, or GPS)
- Receive nearby ride requests (within 5 km) live; accept or decline
- Arrived, start and complete ride actions, SOS alert
- Earnings, ride history, ratings, and a **simulate drive** button for live-tracking demos
- Top-driver ranking by average rating
## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring Web, Spring Data JPA (Hibernate), Spring Security + JWT, Spring WebSocket (STOMP), Bean Validation, Lombok |
| Database | PostgreSQL |
| Payments | Stripe Java SDK (PaymentIntents + signed webhooks) |
| PDF | OpenPDF (iText fork) |
| Frontend | React (Vite), Tailwind CSS, React Router, react-leaflet, @stomp/stompjs, Stripe.js / Elements |

## Architecture

```
React (Vite)
   |  REST (JWT)            WebSocket (STOMP, JWT)
   v                        v
Controllers  ----------  RideSocketController / RideNotifier
   v
Services (business logic: fare, ride lifecycle, payments, ratings, invoice)
   v
Spring Data JPA repositories
   v
PostgreSQL
```

**Ride lifecycle:** `REQUESTED -> ACCEPTED -> STARTED -> COMPLETED` (rider can cancel while `REQUESTED` or `ACCEPTED`).

**Design notes**
- Fare is always calculated and read **on the server**; the client never sends a price.
- Accepting a ride uses a **pessimistic database lock**, so two drivers cannot take the same ride.
- Stripe webhooks are verified with the signing secret and are idempotent; the payment status endpoint also syncs with Stripe as a fallback.
- WebSocket connections require a valid JWT, and a ride's topics can only be subscribed to by its rider and driver.
- Fare formula: base INR 40 + INR 12 per km (minimum INR 60).
## Project Structure

```
cab-booking/
├── cab-booking-backend/
│   └── src/main/java/com/cabbooking/
│       ├── config/        security, CORS, WebSocket, Stripe
│       ├── controller/    REST + WebSocket controllers
│       ├── dto/           request / response records
│       ├── entity/        User, Driver, Ride, Rating, Payment
│       ├── exception/     global error handling
│       ├── repository/    JPA repositories
│       ├── security/      JWT service, filter, user details
│       ├── service/       business logic
│       └── websocket/     STOMP auth interceptor, notifier
└── cab-booking-frontend/
    └── src/
        ├── components/    map, payment, rating, ride details
        ├── hooks/         WebSocket hooks
        └── pages/         login, register, rider and driver dashboards
```

## Getting Started

### Prerequisites
- Java 21, Node 20+
- PostgreSQL (create an empty database named `cabbooking`)
- A free [Stripe](https://stripe.com) account (test mode) and the [Stripe CLI](https://docs.stripe.com/stripe-cli)
### 1. Backend

Set these environment variables (they are read by `application.properties`):

| Variable | Description |
|---|---|
| `DB_PASSWORD` | PostgreSQL password (defaults to `postgres`) |
| `JWT_SECRET` | Any random string, at least 32 characters |
| `STRIPE_SECRET_KEY` | Stripe test secret key (`sk_test_...`) |
| `STRIPE_WEBHOOK_SECRET` | Signing secret printed by `stripe listen` (`whsec_...`) |

```bash
cd cab-booking-backend
./mvnw spring-boot:run        # Windows: .\mvnw spring-boot:run
```
The API runs on `http://localhost:8080`. Tables are created automatically on first start.

### 2. Stripe webhook (in a separate terminal)

```bash
stripe login
stripe listen --events payment_intent.succeeded,payment_intent.payment_failed --forward-to localhost:8080/api/payments/webhook
```

### 3. Frontend

Create `cab-booking-frontend/.env`:

```
VITE_API_URL=http://localhost:8080
VITE_STRIPE_PK=pk_test_your_publishable_key
```

```bash
cd cab-booking-frontend
npm install
npm run dev
```
Open `http://localhost:5173`.

## Try It

The login is stored in the browser, so use a normal window for the rider and an **incognito window for the driver**.

1. Register a **driver** (incognito), set a location, and click **Go online**.
2. Register a **rider**, search or click the map for pickup and drop (pickup within 5 km of the driver), and book.
3. The driver sees the request instantly and accepts it.
4. Driver: **Simulate drive to pickup**, **I've arrived**, **Start ride**, **Simulate drive to drop**, **Complete ride**. The rider watches everything live.
5. Rider: pay with test card `4242 4242 4242 4242` (any future date, any CVC), download the invoice, and rate the driver.
## API Reference

**Auth**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a rider or driver |
| POST | `/api/auth/login` | Login, returns a JWT |
| GET | `/api/me` | Current user |

**Rider** (role `RIDER`)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/rides/estimate` | Distance and fare estimate |
| POST | `/api/rides/book` | Book a ride |
| GET | `/api/rides/my` | Ride history |
| GET | `/api/rides/{id}` | Ride details (rider or its driver) |
| POST | `/api/rides/{id}/cancel` | Cancel a ride |
| POST | `/api/rides/{id}/rate` | Rate a completed ride |
| GET | `/api/rides/{id}/invoice` | Download invoice PDF |
| POST | `/api/payments/ride/{rideId}/intent` | Create a Stripe PaymentIntent |
| GET | `/api/payments/ride/{rideId}` | Payment status |

**Driver** (role `DRIVER`)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/driver/me` | Driver profile |
| PUT | `/api/driver/status?available=true` | Go online / offline |
| PUT | `/api/driver/location` | Update location |
| GET | `/api/driver/requests` | Nearby ride requests |
| POST | `/api/driver/rides/{id}/accept` / `decline` / `start` / `complete` | Ride actions |
| GET | `/api/driver/rides` | Ride history |
| GET | `/api/driver/earnings` | Earnings summary |
| GET | `/api/driver/ratings` | Ratings received |

**Other**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/drivers/top` | Top drivers by rating |
| GET | `/api/health` | Health check |
| POST | `/api/payments/webhook` | Stripe webhook (signature verified) |

**WebSocket** (STOMP endpoint `/ws`, JWT in the `Authorization` header of the CONNECT frame)

| Direction | Destination | Purpose |
|---|---|---|
| Subscribe | `/topic/ride/{id}/status` | Status changes, driver arrived, payment received |
| Subscribe | `/topic/ride/{id}/location` | Live driver location |
| Subscribe | `/topic/ride/{id}/sos` | SOS alerts |
| Subscribe | `/topic/drivers/requests` | New ride requests (drivers only) |
| Subscribe | `/user/queue/errors` | Errors for your own messages |
| Send | `/app/ride/{id}/location`, `/arrived`, `/sos` | Driver location, arrival, SOS |

## Database

Tables: `users`, `drivers`, `rides`, `ratings`, `payments`.
One user has many rides, one driver has many rides, one ride has one payment and one rating.

## Not Included (Future Work)

Redis caching, Kafka ride events, Google OAuth2 login, email receipts - will include in future.

## Notes

- Stripe runs in **test mode** only; no real money is processed.
- Place search and reverse geocoding use OpenStreetMap Nominatim, which is rate limited and meant for light use.
- Never commit real keys; all secrets are read from environment variables.
