from __future__ import annotations

from typing import Iterable

import pandas as pd
from prophet import Prophet

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
