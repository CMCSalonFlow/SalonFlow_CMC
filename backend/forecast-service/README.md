# SalonFlow Forecast Service

FastAPI service for daily revenue forecasting with Prophet.

## Run locally

```powershell
cd forecast-service
python -m venv .venv
.\\.venv\\Scripts\\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001
```

Swagger UI:

```text
http://localhost:8001/docs
```

## Forecast directly from 6 months of history

```powershell
Invoke-RestMethod -Method Post http://localhost:8001/forecast/revenue `
  -ContentType "application/json" `
  -Body '{
    "salonId": "salon-001",
    "periods": 7,
    "intervalWidth": 0.8,
    "history": [
      { "date": "2026-02-01", "revenue": 1200000 },
      { "date": "2026-02-02", "revenue": 980000 },
      { "date": "2026-02-03", "revenue": 1320000 }
    ]
  }'
```

The real payload should contain at least 30 daily points. Six months is recommended.

## Train and reuse a saved model

Train:

```text
POST /models/revenue/train
```

Forecast from saved model:

```text
POST /models/revenue/{salon_id}/forecast?periods=7
```

Saved models are written to:

```text
forecast-service/models
```

## Response shape for chart

Use:

- Actual line: request `history[].date` and `history[].revenue`, color blue.
- Forecast line: response `forecast[].date` and `forecast[].yhat`, color purple.
- Confidence interval: shaded area between `yhat_lower` and `yhat_upper`.

## Java backend endpoints

The Java backend aggregates successful payments from PostgreSQL and calls this service. FE should call Java, not this Python service directly:

```text
GET /api/v1/branches/{branchId}/revenue/history?months=6
GET /api/v1/branches/{branchId}/revenue/forecast?months=6&periods=7
POST /api/v1/branches/{branchId}/revenue/forecast/train?months=6
GET /api/v1/branches/{branchId}/revenue/forecast/saved?months=6&periods=7
```

If Java runs locally, keep:

```properties
FORECAST_SERVICE_BASE_URL=http://localhost:8001
```

If Java runs inside Docker on the same compose network, use:

```properties
FORECAST_SERVICE_BASE_URL=http://forecast-service:8000
```

## Monthly retraining

The Java backend registers a monthly job at 02:00 on day 1, Asia/Bangkok time. It aggregates the latest six months of successful payments for each branch and calls `POST /models/revenue/train`.

`app/scheduler.py` also contains a Python-side hook, but the Java scheduler is the primary retraining path because Java owns the database access.
