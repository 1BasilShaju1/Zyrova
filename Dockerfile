# ---- build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY . /ctx
RUN set -e; \
    SRC="$(find /ctx -name ZyrovaWeb.java -not -path '*/.git/*' | head -n1)"; \
    HTML="$(find /ctx -name index.html -not -path '*/.git/*' | head -n1)"; \
    test -n "$SRC"  || { echo "ERROR: ZyrovaWeb.java not found in repo"; exit 1; }; \
    test -n "$HTML" || { echo "ERROR: index.html not found in repo (expected public/index.html)"; exit 1; }; \
    mkdir -p out public; \
    javac -encoding UTF-8 -d out "$SRC"; \
    cp "$HTML" public/index.html

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/out ./classes
COPY --from=build /build/public ./public
ENV PORT=10000
EXPOSE 10000
CMD ["java", "-Xmx300m", "-cp", "classes", "ZyrovaWeb"]
