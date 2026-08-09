from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path

from prophet.serialize import model_from_json, model_to_json

from app.forecasting import train_model
from app.schemas import RevenuePoint


MODEL_DIR = Path(__file__).resolve().parent.parent / "models"
MODEL_VERSION = "revenue_prophet_v1"


def model_path_for(salon_id: str) -> Path:
    safe_salon_id = "".join(char if char.isalnum() or char in ("-", "_") else "_" for char in salon_id)
    return MODEL_DIR / f"{safe_salon_id}.json"


def metadata_path_for(salon_id: str) -> Path:
    safe_salon_id = "".join(char if char.isalnum() or char in ("-", "_") else "_" for char in salon_id)
    return MODEL_DIR / f"{safe_salon_id}.metadata.json"


def save_model(
    salon_id: str,
    history: list[RevenuePoint],
    interval_width: float,
    training_months: int | None = None,
) -> Path:
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    model = train_model(history, interval_width=interval_width)
    path = model_path_for(salon_id)
    path.write_text(model_to_json(model), encoding="utf-8")
    metadata_path_for(salon_id).write_text(
        json.dumps(
            {
                "salonId": salon_id,
                "trained": True,
                "lastTrainedAt": datetime.now(timezone.utc).isoformat(),
                "trainingMonths": training_months,
                "dataPoints": len(history),
                "modelVersion": MODEL_VERSION,
            },
            ensure_ascii=True,
        ),
        encoding="utf-8",
    )
    return path


def load_model(salon_id: str):
    path = model_path_for(salon_id)
    if not path.exists():
        return None
    return model_from_json(path.read_text(encoding="utf-8"))


def load_model_status(salon_id: str) -> dict | None:
    if not model_path_for(salon_id).exists():
        return None

    path = metadata_path_for(salon_id)
    if not path.exists():
        return {
            "salonId": salon_id,
            "trained": True,
            "lastTrainedAt": None,
            "trainingMonths": None,
            "dataPoints": None,
            "modelVersion": MODEL_VERSION,
        }
    return json.loads(path.read_text(encoding="utf-8"))
