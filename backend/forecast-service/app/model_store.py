from __future__ import annotations

from pathlib import Path

from prophet.serialize import model_from_json, model_to_json

from app.forecasting import train_model
from app.schemas import RevenuePoint


MODEL_DIR = Path(__file__).resolve().parent.parent / "models"


def model_path_for(salon_id: str) -> Path:
    safe_salon_id = "".join(char if char.isalnum() or char in ("-", "_") else "_" for char in salon_id)
    return MODEL_DIR / f"{safe_salon_id}.json"


def save_model(salon_id: str, history: list[RevenuePoint], interval_width: float) -> Path:
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    model = train_model(history, interval_width=interval_width)
    path = model_path_for(salon_id)
    path.write_text(model_to_json(model), encoding="utf-8")
    return path


def load_model(salon_id: str):
    path = model_path_for(salon_id)
    if not path.exists():
        return None
    return model_from_json(path.read_text(encoding="utf-8"))
