# ---- build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY src/ ./src/
RUN mkdir out && javac -encoding UTF-8 -d out src/ZyrovaWeb.java

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/out ./classes
COPY public ./public
ENV PORT=10000
EXPOSE 10000
CMD ["java", "-Xmx300m", "-cp", "classes", "ZyrovaWeb"]
