from __future__ import annotations

import logging

from apscheduler.schedulers.background import BackgroundScheduler


logger = logging.getLogger(__name__)


def retrain_monthly_models() -> None:
    logger.info("Monthly retrain hook started")
    # TODO: Fetch the latest 6 months of daily revenue per salon from the Java backend
    # or database, then call model_store.save_model for each salon.
    logger.info("Monthly retrain hook finished")


def start_scheduler() -> BackgroundScheduler:
    scheduler = BackgroundScheduler(timezone="Asia/Bangkok")
    scheduler.add_job(
        retrain_monthly_models,
        "cron",
        day=1,
        hour=2,
        minute=0,
        id="monthly_revenue_forecast_retrain",
        replace_existing=True,
    )
    scheduler.start()
    return scheduler
