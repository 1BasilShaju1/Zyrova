# ZYROVA - Premium Vehicle Rentals

A vehicle rental management app with a gold-on-dark UI. It ships in two editions:

| Edition | Tech | Where it runs |
|---|---|---|
| **Desktop** | Java Swing (`desktop/ZYROVA.java`) | Your computer |
| **Web** | Java `HttpServer` + HTML/CSS/JS | Render (Docker) |

## Features
- 36-vehicle fleet across **Premium**, **Luxury** and **Sports & Supercars**
- Filter by category and search by name or type
- Rent a vehicle with live total, validation and a printable-style receipt
- Return vehicles from the Fleet or Bookings tab
- Bookings overview with total active value

## Project structure
```
zyrova/
├── src/ZyrovaWeb.java     # Web server, model, data and REST API
├── public/index.html      # Single-page frontend
├── Dockerfile             # Multi-stage build (JDK -> JRE)
├── render.yaml            # Render blueprint
└── desktop/ZYROVA.java    # Original Swing app (optional, not deployed)
```

## Run locally
Requires JDK 17+.
```bash
javac -encoding UTF-8 -d out src/ZyrovaWeb.java
java -cp out ZyrovaWeb
```
Open http://localhost:10000

The desktop edition: `javac desktop/ZYROVA.java && java -cp desktop ZYROVA`

## Deploy to Render
1. Push this repo to GitHub (`Dockerfile` and `render.yaml` must be in the repo root).
2. In Render: **New -> Blueprint**, select the repo, and apply.
   (Or **New -> Web Service**, runtime **Docker**, health check path `/health`.)
3. Open the generated `onrender.com` URL.

No environment variables are needed; Render provides `PORT`.

## API
| Method | Path | Body (form-encoded) |
|---|---|---|
| GET | `/api/vehicles` | - |
| GET | `/api/bookings` | - |
| POST | `/api/rent` | `id`, `customer`, `phone`, `days` |
| POST | `/api/return` | `id` |
| GET | `/health` | - |

## Notes
- Data is stored in memory and resets on restart or redeploy.
- The Render free plan sleeps after ~15 minutes idle; the first request afterwards is slow.
- There is no authentication; add one before real-world use.
