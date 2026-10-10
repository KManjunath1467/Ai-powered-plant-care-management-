import os
import joblib
import pandas as pd
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

# Load the trained RandomForestRegressor model if present
MODEL_PATH = os.path.join(os.path.dirname(__file__), "watering_model.pkl")
model = None

try:
    if os.path.exists(MODEL_PATH):
        model = joblib.load(MODEL_PATH)
        print("Successfully loaded RandomForest watering_model.pkl")
    else:
        print("Warning: watering_model.pkl not found at", MODEL_PATH)
except Exception as e:
    print("Warning: could not load model:", e)

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status": "healthy",
        "service": "MyPlant AI Python Service",
        "modelLoaded": model is not None
    })

@app.route('/predict', methods=['GET'])
def predict():
    try:
        # Get query parameters safely
        temperature = request.args.get('temperature')
        humidity = request.args.get('humidity')
        plant_type = request.args.get('plantType', 0)

        # Validation
        if temperature is None or humidity is None:
            return jsonify({
                "error": "temperature and humidity are required"
            }), 400

        temperature = float(temperature)
        humidity = float(humidity)
        try:
            plant_type_val = int(plant_type)
        except ValueError:
            plant_type_val = 0

        # Predict using trained RandomForest model if available
        if model is not None:
            try:
                features = pd.DataFrame([{
                    "temperature": temperature,
                    "humidity": humidity,
                    "plant_type": plant_type_val
                }])
                pred = model.predict(features)[0]
                watering_days = max(1.0, float(pred))
                return jsonify({
                    "recommendedWateringDays": round(watering_days, 1),
                    "plantType": plant_type,
                    "engine": "RandomForestRegressor"
                })
            except Exception as ml_err:
                print("Model prediction error, using heuristic fallback:", ml_err)

        # Deterministic botanical formula fallback
        watering_days = 7.0 - (temperature / 10.0) + (humidity / 20.0)
        watering_days = max(1.0, watering_days)

        return jsonify({
            "recommendedWateringDays": round(watering_days, 1),
            "plantType": plant_type,
            "engine": "BotanicalHeuristic"
        })

    except Exception as e:
        print("ERROR in /predict:", e)
        return jsonify({
            "error": str(e)
        }), 500

if __name__ == '__main__':
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)