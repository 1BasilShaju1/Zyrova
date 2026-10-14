# ZYROVA on Render

Keep your original `ZYROVA.java` (Swing) in a `desktop/` folder if you like. Render deploys only the web edition.

## Deploy
1. Push this folder to a GitHub repo.
2. Render -> New -> Blueprint (reads `render.yaml`), or New -> Web Service -> Runtime: Docker.
3. Health check path: `/health`. No env vars needed (Render sets `PORT`).

## Run locally
    javac -encoding UTF-8 -d out src/ZyrovaWeb.java && java -cp out ZyrovaWeb

## Notes
- Data is in memory, like the Swing app: it resets on restart, and the free plan sleeps after ~15 min idle.
