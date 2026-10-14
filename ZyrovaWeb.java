import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;

/** ZYROVA web edition: same model, data and rules as the Swing app, served over HTTP. */
public class ZyrovaWeb {

    // ---------- Model (same fields as the Swing version) ----------
    static class Vehicle {
        int id; String name, type, category; double pricePerDay; boolean available = true;
        Vehicle(int id, String name, String type, String category, double pricePerDay) {
            this.id = id; this.name = name; this.type = type; this.category = category; this.pricePerDay = pricePerDay;
        }
    }

    static class Booking {
        Vehicle vehicle; String customer, phone; int days; double total;
        Booking(Vehicle v, String customer, String phone, int days) {
            this.vehicle = v; this.customer = customer; this.phone = phone; this.days = days;
            this.total = v.pricePerDay * days;
        }
    }

    static final List<Vehicle> vehicles = new ArrayList<>();
    static final List<Booking> bookings = new ArrayList<>();
    static final Object LOCK = new Object();

    static void add(int id, String name, String type, String cat, double price) {
        vehicles.add(new Vehicle(id, name, type, cat, price));
    }

    static void loadVehicles() {
        add(1, "Maruti Suzuki Grand Vitara", "Premium SUV", "Premium", 2500);
        add(2, "Hyundai Tucson", "Premium SUV", "Premium", 3000);
        add(3, "Kia Carnival", "Premium MPV", "Premium", 4000);
        add(4, "Toyota Fortuner", "Premium SUV", "Premium", 4000);
        add(5, "Jeep Meridian", "Premium SUV", "Premium", 3500);
        add(6, "Jeep Wrangler", "Premium 4x4", "Premium", 6000);
        add(7, "Skoda Kodiaq", "Premium SUV", "Premium", 4000);
        add(8, "Volkswagen Tiguan", "Premium SUV", "Premium", 3500);
        add(9, "Toyota Innova Hycross", "Premium MPV", "Premium", 3500);
        add(10, "Mahindra Thar Roxx", "Premium SUV", "Premium", 3500);
        add(11, "Audi A4", "Luxury Sedan", "Luxury", 7500);
        add(12, "Audi Q3", "Luxury SUV", "Luxury", 8000);
        add(13, "Audi Q5", "Luxury SUV", "Luxury", 10000);
        add(14, "BMW 3 Series", "Luxury Sedan", "Luxury", 8000);
        add(15, "BMW 5 Series", "Luxury Sedan", "Luxury", 12000);
        add(16, "BMW X3", "Luxury SUV", "Luxury", 10000);
        add(17, "Mercedes-Benz C-Class", "Luxury Sedan", "Luxury", 9000);
        add(18, "Mercedes-Benz E-Class", "Luxury Sedan", "Luxury", 12000);
        add(19, "Mercedes-Benz GLC", "Luxury SUV", "Luxury", 12000);
        add(20, "Volvo XC60", "Luxury SUV", "Luxury", 10000);
        add(21, "Land Rover Defender", "Luxury 4x4", "Luxury", 15000);
        add(22, "Range Rover Velar", "Luxury SUV", "Luxury", 18000);
        add(23, "Jaguar F-Pace", "Luxury SUV", "Luxury", 13000);
        add(24, "Porsche Macan", "Performance SUV", "Sports & Supercars", 18000);
        add(25, "Porsche 718 Cayman", "Sports Car", "Sports & Supercars", 25000);
        add(26, "Porsche 911 Carrera", "Sports Car", "Sports & Supercars", 35000);
        add(27, "BMW M4", "Performance Coupe", "Sports & Supercars", 18000);
        add(28, "BMW M5", "Performance Sedan", "Sports & Supercars", 22000);
        add(29, "Mercedes-AMG C63", "Performance Sedan", "Sports & Supercars", 20000);
        add(30, "Audi RS5", "Performance Coupe", "Sports & Supercars", 20000);
        add(31, "Audi RS Q8", "Performance SUV", "Sports & Supercars", 25000);
        add(32, "Ford Mustang", "Muscle/Sports", "Sports & Supercars", 20000);
        add(33, "Chevrolet Corvette", "Sports Car", "Sports & Supercars", 30000);
        add(34, "Lamborghini Hurac\u00e1n", "Supercar", "Sports & Supercars", 60000);
        add(35, "Ferrari 488", "Supercar", "Sports & Supercars", 75000);
        add(36, "McLaren 720S", "Supercar", "Sports & Supercars", 80000);
    }

    static Vehicle find(int id) {
        for (Vehicle v : vehicles) if (v.id == id) return v;
        return null;
    }

    // ---------- JSON helpers ----------
    static String q(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    static String json(Vehicle v) {
        return "{\"id\":" + v.id + ",\"name\":" + q(v.name) + ",\"type\":" + q(v.type) + ",\"category\":" + q(v.category)
                + ",\"pricePerDay\":" + (long) v.pricePerDay + ",\"available\":" + v.available + "}";
    }

    static String json(Booking b) {
        return "{\"vehicleId\":" + b.vehicle.id + ",\"vehicle\":" + q(b.vehicle.name) + ",\"type\":" + q(b.vehicle.type)
                + ",\"category\":" + q(b.vehicle.category) + ",\"pricePerDay\":" + (long) b.vehicle.pricePerDay
                + ",\"customer\":" + q(b.customer) + ",\"phone\":" + q(b.phone)
                + ",\"days\":" + b.days + ",\"total\":" + (long) b.total + "}";
    }

    static Map<String, String> form(HttpExchange x) throws IOException {
        String body = new String(x.getRequestBody().readNBytes(10_000), StandardCharsets.UTF_8);
        Map<String, String> m = new HashMap<>();
        for (String p : body.split("&")) {
            if (p.isEmpty()) continue;
            int i = p.indexOf('=');
            String k = URLDecoder.decode(i < 0 ? p : p.substring(0, i), StandardCharsets.UTF_8);
            String v = i < 0 ? "" : URLDecoder.decode(p.substring(i + 1), StandardCharsets.UTF_8);
            m.put(k, v);
        }
        return m;
    }

    static void send(HttpExchange x, int code, String type, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.sendResponseHeaders(code, b.length);
        try (var os = x.getResponseBody()) { os.write(b); }
    }

    static void sendJson(HttpExchange x, int code, String body) throws IOException {
        send(x, code, "application/json", body);
    }

    static void error(HttpExchange x, int code, String msg) throws IOException {
        sendJson(x, code, "{\"error\":" + q(msg) + "}");
    }

    // ---------- Routing ----------
    static void handle(HttpExchange x) throws IOException {
        try {
            String path = x.getRequestURI().getPath();
            String method = x.getRequestMethod();

            if (path.equals("/health")) { send(x, 200, "text/plain", "ok"); return; }

            if (path.equals("/") || path.equals("/index.html")) {
                Path f = Path.of(System.getenv().getOrDefault("PUBLIC_DIR", "public"), "index.html");
                send(x, 200, "text/html", Files.readString(f));
                return;
            }

            if (path.equals("/api/vehicles") && method.equals("GET")) {
                synchronized (LOCK) {
                    StringJoiner j = new StringJoiner(",", "[", "]");
                    for (Vehicle v : vehicles) j.add(json(v));
                    sendJson(x, 200, j.toString());
                }
                return;
            }

            if (path.equals("/api/bookings") && method.equals("GET")) {
                synchronized (LOCK) {
                    StringJoiner j = new StringJoiner(",", "[", "]");
                    for (Booking b : bookings) j.add(json(b));
                    sendJson(x, 200, j.toString());
                }
                return;
            }

            if (path.equals("/api/rent") && method.equals("POST")) {
                Map<String, String> f = form(x);
                String cn = f.getOrDefault("customer", "").trim();
                String ph = f.getOrDefault("phone", "").trim();
                int id, days;
                try {
                    id = Integer.parseInt(f.getOrDefault("id", ""));
                    days = Integer.parseInt(f.getOrDefault("days", ""));
                } catch (NumberFormatException e) { error(x, 400, "Invalid request."); return; }

                if (cn.isEmpty() || cn.length() > 80) { error(x, 400, "Please enter the customer name."); return; }
                if (!ph.matches("[0-9+ \\-]{7,15}")) { error(x, 400, "Please enter a valid phone number."); return; }
                if (days < 1 || days > 365) { error(x, 400, "Rental days must be between 1 and 365."); return; }

                synchronized (LOCK) {
                    Vehicle v = find(id);
                    if (v == null) { error(x, 404, "Vehicle not found."); return; }
                    if (!v.available) { error(x, 409, v.name + " is already rented."); return; }
                    Booking b = new Booking(v, cn, ph, days);
                    v.available = false;
                    bookings.add(b);
                    sendJson(x, 200, json(b));
                }
                return;
            }

            if (path.equals("/api/return") && method.equals("POST")) {
                int id;
                try { id = Integer.parseInt(form(x).getOrDefault("id", "")); }
                catch (NumberFormatException e) { error(x, 400, "Invalid request."); return; }
                synchronized (LOCK) {
                    Vehicle v = find(id);
                    if (v == null) { error(x, 404, "Vehicle not found."); return; }
                    if (v.available) { error(x, 409, "This vehicle is already available."); return; }
                    v.available = true;
                    bookings.removeIf(b -> b.vehicle == v);
                    sendJson(x, 200, "{\"ok\":true,\"name\":" + q(v.name) + "}");
                }
                return;
            }

            error(x, 404, "Not found.");
        } catch (Exception e) {
            e.printStackTrace();
            try { error(x, 500, "Server error."); } catch (Exception ignored) { }
        } finally {
            x.close();
        }
    }

    public static void main(String[] args) throws Exception {
        loadVehicles();
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "10000"));
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", ZyrovaWeb::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("ZYROVA web running on port " + port);
    }
}
