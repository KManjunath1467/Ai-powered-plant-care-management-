# 🌱 AI-Powered Smart Plant Care Management System with Groq Integration

An intelligent botanical care platform featuring Groq LLM intelligence, computer-aided botanical taxonomy & anatomy, real-time Open-Meteo weather telemetry, IoT sensor rod integration (ESP32/ESP8266), evidence-based watering recommendations, and multi-plant management.

---

## 🚀 Key Architecture & Upgrades

### 1. 🧠 Groq API Integration (Primary LLM Provider)
* **Secure Environment Configuration:** Configured strictly server-side via `GROQ_API_KEY` and `GROQ_MODEL` (e.g. `llama-3.3-70b-versatile` or `llama-3.1-8b-instant`). Never exposed to browser or frontend bundles.
* **Resilient Service Design:** `GroqAIService` provides structured JSON output validation, controlled timeouts (15s), exponential backoff retries, and rate-limit handling.
* **Graceful Degradation:** When the Groq API key is unconfigured or rate-limited, the application degrades gracefully to a deterministic botanical rule engine without crashing.
* **Interactive Plant Assistant:** Conversational natural-language chat on every plant detail page strictly grounded in the plant's profile, live weather, and IoT sensor telemetry.
* **Botanical Anatomy Breakdown:** Generates plant-specific morphology covering **Roots**, **Stem/Trunk**, **Leaves & Lamina**, **Floral Organs**, **Fruits & Seeds**, **Buds & Meristems**, and **Vascular Tissues (Xylem & Phloem)**.
* **Disease Diagnostic Explanations:** Explains observed symptoms, possible causes, alternatives, and preventive actions without making fabricated medical claims.

---

### 2. 📡 IoT Sensor Rod (ESP32 / ESP8266) & Mandatory Empty-State
* **Telemetry Parameters:**
  * Soil Moisture (%)
  * Soil Temperature (°C)
  * Soil Electrical Conductivity (EC in mS/cm or µS/cm)
  * Soil Substrate pH (0-14)
  * Canopy Ambient Air Temperature (°C)
  * Ambient Relative Humidity (%)
  * Light Intensity (lux)
* **Strict Empty-State Enforcement:**
  * When a plant has **no paired sensor rod**, the sensor health dashboard remains strictly empty of measurements.
  * Explicitly displays: *"Connect a compatible sensor rod to enable soil-based health monitoring for this plant."*
  * Never fabricates fake moisture, pH, or EC values.
* **Stale Telemetry Protection:** If a sensor rod has not transmitted readings within 2 hours, telemetry is clearly marked as stale with last recorded timestamps.
* **Telemetry Ingestion Endpoint:** `POST /api/sensors/{deviceId}/readings` authenticated via pairing token.

---

### 3. 🌦️ Live Location & Weather (Open-Meteo API)
* **High-Precision Telemetry:** Sourced directly from Open-Meteo (free, zero API key requirement, global coverage) with OpenWeatherMap fallback.
* **Micro-Climate Monitoring:** Supports independent GPS coordinates or cities for each individual plant.
* **Extended Forecasts:** Provides 24-hour hourly outlook and 7-day extended agricultural forecast with precipitation probabilities and wind speeds.
* **Rate-Limit Caching:** 15-minute in-memory caching prevents redundant external API traffic.

---

### 4. 💧 Evidence-Grounded Watering Recommendations
Combines:
* Plant profile & species base interval
* Actual soil moisture (when sensor rod is paired)
* Recent watering history & streak
* Ambient temperature and forecast rainfall
* Evaluated through deterministic threshold rules with Groq natural-language explanations.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Frontend** | React 18, Vite, Tailwind CSS, Lucide Icons, Framer Motion, Axios |
| **Backend** | Java 17+, Spring Boot 3.3.5, Spring Security, JWT, WebFlux, JPA / Hibernate |
| **AI & LLM** | Groq Cloud API (`llama-3.3-70b-versatile`), Python Flask ML Service (`RandomForestRegressor`) |
| **Database** | MySQL 8.0+ |
| **IoT Telemetry** | HTTP REST endpoints for ESP32 / ESP8266 microcontroller sensor rods |
| **Weather** | Open-Meteo Meteorological API |

---

## ⚙️ Environment Configuration

### 1. Backend `.env` Configuration
Copy `backend/.env.example` to `backend/.env` or configure system environment variables:

```bash
# ======================================
# Groq AI Cloud Provider (Mandatory for Live LLM)
# ======================================
GROQ_API_KEY=gsk_your_groq_api_key_here
GROQ_MODEL=llama-3.3-70b-versatile

# ======================================
# MySQL Database
# ======================================
DB_URL=jdbc:mysql://localhost:3306/myplant?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_password_here

# ======================================
# JWT Secret
# ======================================
JWT_SECRET=MyPlantSecretKey12345678901234567890123456789012345678901234567890
JWT_EXPIRATION=86400000

# ======================================
# Email & WhatsApp (Optional)
# ======================================
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-google-app-password
TWILIO_ACCOUNT_SID=your_twilio_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
```

### 🔑 How to Obtain a Free Groq API Key:
1. Visit the [Groq Console](https://console.groq.com/keys).
2. Sign in with your Google or GitHub account.
3. Click **Create API Key**, name it `myplant-smart-care`, and copy the generated key (`gsk_...`).
4. Set `GROQ_API_KEY=gsk_...` in your backend environment.

---

## 🚀 Step-by-Step Local Setup

### Step 1: Database Setup (MySQL)
Ensure MySQL is running on port 3306 and execute:
```sql
CREATE DATABASE IF NOT EXISTS myplant;
```
Tables and schema updates will automatically synchronize via Hibernate DDL.

### Step 2: Start the Python AI Service
```bash
cd ai-service
pip install -r package.json # or: pip install flask flask-cors pandas scikit-learn joblib groq
python train_model.py      # Trains and generates watering_model.pkl
python app.py              # Starts Flask AI service on port 5000
```

### Step 3: Start the Spring Boot Backend
```bash
cd backend
mvn spring-boot:run        # Starts API server on http://localhost:8081
```

### Step 4: Start the React Frontend
```bash
cd frontend
npm install
npm run dev                # Starts Vite dev server on http://localhost:5173
```

---

## 🧪 Testing and Verification

* **Unit Tests:** Execute `mvn test` in the `backend/` directory to run JUnit tests verifying Groq taxonomy fallbacks, botanical anatomy breakdown, sensor empty-state logic, and evidence validation.
* **IoT Hardware Testing:** Use the **Test Hardware Reading** button in the *Sensor Rods (IoT)* dashboard to ingest test telemetry payloads without physical hardware.
* **Identification Testing:** Upload any plant photo in the *Identify & Anatomy* dashboard to see real-time Groq taxonomy classification and morphological tissue breakdown.
