from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException

from app.forecasting import predict_next_days, train_model
from app.model_store import load_model, save_model
from app.scheduler import start_scheduler
from app.schemas import (
    ForecastRequest,
    ForecastResponse,
    HealthResponse,
    TrainRequest,
    TrainResponse,
)


logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    scheduler = start_scheduler()
    logger.info("Forecast scheduler started")
    yield
    scheduler.shutdown(wait=False)


app = FastAPI(
    title="SalonFlow Forecast Service",
    version="1.0.0",
    description="Revenue forecasting service powered by Prophet.",
    lifespan=lifespan,
)


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok")


@app.post("/forecast/revenue", response_model=ForecastResponse)
def forecast_revenue(request: ForecastRequest) -> ForecastResponse:
    model = train_model(request.history, interval_width=request.interval_width)
    forecast = predict_next_days(model, request.periods)
    return ForecastResponse(
        salon_id=request.salon_id,
        periods=request.periods,
        forecast=forecast,
    )


@app.post("/models/revenue/train", response_model=TrainResponse)
def train_revenue_model(request: TrainRequest) -> TrainResponse:
    path = save_model(
        salon_id=request.salon_id,
        history=request.history,
        interval_width=request.interval_width,
    )
    return TrainResponse(
        salon_id=request.salon_id,
        model_path=str(path),
        trained_points=len(request.history),
    )


@app.post("/models/revenue/{salon_id}/forecast", response_model=ForecastResponse)
def forecast_with_saved_model(salon_id: str, periods: int = 7) -> ForecastResponse:
    if periods < 1 or periods > 30:
        raise HTTPException(status_code=422, detail="periods must be between 1 and 30")

    model = load_model(salon_id)
    if model is None:
        raise HTTPException(status_code=404, detail="trained model not found")

    forecast = predict_next_days(model, periods)
    return ForecastResponse(
        salon_id=salon_id,
        periods=periods,
        forecast=forecast,
    )
