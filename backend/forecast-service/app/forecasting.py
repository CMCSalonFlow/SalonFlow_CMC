from __future__ import annotations

from typing import Iterable

import pandas as pd
from prophet import Prophet
import io
import base64
from sklearn.metrics import mean_absolute_error, mean_absolute_percentage_error
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

from app.schemas import ForecastPoint, RevenuePoint


def build_history_frame(history: Iterable[RevenuePoint]) -> pd.DataFrame:
    frame = pd.DataFrame(
        [{"ds": point.date, "y": point.revenue} for point in history],
    )
    frame["ds"] = pd.to_datetime(frame["ds"])
    frame = frame.sort_values("ds").drop_duplicates(subset=["ds"], keep="last")

    daily_index = pd.date_range(frame["ds"].min(), frame["ds"].max(), freq="D")
    frame = (
        frame.set_index("ds")
        .reindex(daily_index)
        .rename_axis("ds")
        .reset_index()
    )
    frame["y"] = frame["y"].fillna(0)
    return frame


def create_model(interval_width: float = 0.8) -> Prophet:
    return Prophet(
        interval_width=interval_width,
        weekly_seasonality=True,
        yearly_seasonality=False,
        daily_seasonality=False,
    )


def train_model(history: Iterable[RevenuePoint], interval_width: float = 0.8) -> Prophet:
    frame = build_history_frame(history)
    model = create_model(interval_width=interval_width)
    model.fit(frame)
    return model


def predict_next_days(model: Prophet, periods: int) -> list[ForecastPoint]:
    future = model.make_future_dataframe(periods=periods, freq="D", include_history=False)
    forecast = model.predict(future)
    return [
        ForecastPoint(
            date=row.ds.date(),
            yhat=max(float(row.yhat), 0.0),
            yhat_lower=max(float(row.yhat_lower), 0.0),
            yhat_upper=max(float(row.yhat_upper), 0.0),
        )
        for row in forecast[["ds", "yhat", "yhat_lower", "yhat_upper"]].itertuples(index=False)
    ]


def evaluate_metrics(history: Iterable[RevenuePoint], interval_width: float = 0.8) -> tuple[float | None, float | None]:
    frame = build_history_frame(history)
    if len(frame) < 14:
        return None, None
        
    train_size = len(frame) - 7
    train_df = frame.iloc[:train_size]
    test_df = frame.iloc[train_size:]
    
    model = create_model(interval_width=interval_width)
    model.fit(train_df)
    
    future = model.make_future_dataframe(periods=7, freq="D", include_history=False)
    forecast = model.predict(future)
    
    predictions = forecast['yhat'].values
    actuals = test_df['y'].values
    
    mae = mean_absolute_error(actuals, predictions)
    mape = mean_absolute_percentage_error(actuals, predictions)
    
    return float(mae), float(mape)


def generate_forecast_chart(history: Iterable[RevenuePoint], forecast: list[ForecastPoint]) -> str:
    frame = build_history_frame(history)
    
    forecast_dates = [pd.to_datetime(p.date) for p in forecast]
    forecast_yhat = [p.yhat for p in forecast]
    forecast_lower = [p.yhat_lower for p in forecast]
    forecast_upper = [p.yhat_upper for p in forecast]
    
    plt.figure(figsize=(10, 5))
    
    plt.plot(frame['ds'], frame['y'], label='Actual Revenue', color='blue', marker='o', markersize=4)
    plt.plot(forecast_dates, forecast_yhat, label='Forecast', color='purple', linestyle='--', marker='x', markersize=4)
    plt.fill_between(forecast_dates, forecast_lower, forecast_upper, color='purple', alpha=0.2, label='Confidence Interval')
    
    plt.title('Revenue Forecast (Prophet)')
    plt.xlabel('Date')
    plt.ylabel('Revenue')
    plt.legend()
    plt.grid(True, linestyle=':', alpha=0.6)
    plt.tight_layout()
    
    buf = io.BytesIO()
    plt.savefig(buf, format='png')
    plt.close()
    buf.seek(0)
    
    return base64.b64encode(buf.read()).decode('utf-8')

