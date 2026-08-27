from datetime import date, datetime
from typing import List

from pydantic import BaseModel, ConfigDict, Field, field_validator


class RevenuePoint(BaseModel):
    date: date
    revenue: float = Field(ge=0)


class ForecastRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    salon_id: str = Field(min_length=1, alias="salonId")
    history: List[RevenuePoint] = Field(min_length=14)
    periods: int = Field(default=7, ge=1, le=30)
    interval_width: float = Field(default=0.8, ge=0.5, le=0.99, alias="intervalWidth")

    @field_validator("history")
    @classmethod
    def history_dates_must_be_unique(cls, value: List[RevenuePoint]) -> List[RevenuePoint]:
        dates = [point.date for point in value]
        if len(dates) != len(set(dates)):
            raise ValueError("history dates must be unique")
        return value


class ForecastPoint(BaseModel):
    date: date
    yhat: float
    yhat_lower: float
    yhat_upper: float


class ForecastResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    salon_id: str = Field(alias="salonId")
    periods: int
    forecast: List[ForecastPoint]
    mae: float | None = None
    mape: float | None = None
    chart_base64: str | None = Field(default=None, alias="chartBase64")


class TrainRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    salon_id: str = Field(min_length=1, alias="salonId")
    history: List[RevenuePoint] = Field(min_length=14)
    interval_width: float = Field(default=0.8, ge=0.5, le=0.99, alias="intervalWidth")
    training_months: int | None = Field(default=None, ge=1, le=24, alias="trainingMonths")


class TrainResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    salon_id: str = Field(alias="salonId")
    model_path: str = Field(alias="modelPath")
    trained_points: int = Field(alias="trainedPoints")


class ModelStatusResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    salon_id: str = Field(alias="salonId")
    trained: bool
    last_trained_at: datetime | None = Field(default=None, alias="lastTrainedAt")
    training_months: int | None = Field(default=None, alias="trainingMonths")
    data_points: int | None = Field(default=None, alias="dataPoints")
    model_version: str = Field(default="revenue_prophet_v1", alias="modelVersion")


class HealthResponse(BaseModel):
    status: str
