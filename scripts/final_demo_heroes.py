import csv
import datetime as dt
import json
import math
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = Path(__file__).resolve().parents[1] / "target" / "final-demo-data"
sys.path.insert(0, str(ROOT / "sme-stocksense-ai-forecast"))
from app.forecaster import run_xgboost_forecast  # noqa: E402


def finite(*values):
    return all(v is not None and math.isfinite(float(v)) for v in values)


def main():
    history = defaultdict(list)
    with (OUT / "store-item-history.csv").open(newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            history[(row["product_code"], row["warehouse_code"])].append({
                "date": row["date"],
                "quantity": float(row["quantity"]),
                "price": None if row["average_price"] == "" else float(row["average_price"]),
            })

    inventory = {}
    with (OUT / "demo-inventory.csv").open(newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            inventory[(row["product_code"], row["warehouse_code"])] = {
                "quantity": int(row["quantity"]),
                "min_stock": int(row["min_stock"]),
                "scenario": row["scenario"],
            }

    candidates = []
    for key, rows in history.items():
        result = run_xgboost_forecast(rows, [7, 14, 30])
        daily = [float(p["predicted_quantity"]) for p in result["daily_predictions"]]
        inv = inventory[key]
        demand30 = sum(daily)
        raw = max(0, round(inv["min_stock"] + demand30 - inv["quantity"]))
        crosses = inv["quantity"] > inv["min_stock"] and inv["quantity"] - demand30 < inv["min_stock"]
        healthy = inv["quantity"] - demand30 >= inv["min_stock"]
        if finite(result["smape"], result["mae"], result["rmse"], demand30) and max(daily) < 200:
            candidates.append((key, result, demand30, raw, crosses, healthy, inv))
        if len(candidates) >= 180:
            break

    heroes = {}
    for key, result, demand30, raw, crosses, healthy, inv in candidates:
        if "LOW" not in heroes and inv["quantity"] < inv["min_stock"] and raw > 0:
            heroes["LOW"] = (key, result, demand30, raw, inv)
        elif "NEAR_RISK" not in heroes and crosses and raw > 0:
            heroes["NEAR_RISK"] = (key, result, demand30, raw, inv)
        elif "HEALTHY" not in heroes and healthy and float(result["smape"]) <= 60:
            heroes["HEALTHY"] = (key, result, demand30, raw, inv)
        if len(heroes) == 3:
            break

    missing = {"LOW", "NEAR_RISK", "HEALTHY"} - set(heroes)
    if missing:
        raise SystemExit(f"Missing hero scenarios: {sorted(missing)}")

    now = dt.datetime.now().replace(microsecond=0).isoformat(sep=" ")
    with (OUT / "forecast-models.csv").open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["scenario", "product_code", "warehouse_code", "version", "smape", "mae", "rmse", "data_days", "mode", "dataset_type", "history_start", "history_end", "trained_at"])
        for scenario, (key, result, demand30, raw, inv) in heroes.items():
            rows = history[key]
            w.writerow([scenario, key[0], key[1], 1, result["smape"], result["mae"], result["rmse"], len(rows), "XGBOOST", "EXTERNAL", rows[0]["date"], rows[-1]["date"], now])

    with (OUT / "forecast-results.csv").open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["scenario", "horizon", "forecast_date", "predicted_quantity"])
        for scenario, (key, result, demand30, raw, inv) in heroes.items():
            daily = [float(p["predicted_quantity"]) for p in result["daily_predictions"]]
            base = dt.date.fromisoformat(history[key][-1]["date"])
            for h in (7, 14, 30):
                avg = sum(daily[:h]) / h
                w.writerow([scenario, h, (base + dt.timedelta(days=h)).isoformat(), f"{avg:.4f}"])

    with (OUT / "daily-forecast.csv").open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["scenario", "forecast_date", "predicted_quantity"])
        for scenario, (key, result, demand30, raw, inv) in heroes.items():
            for p in result["daily_predictions"]:
                w.writerow([scenario, p["date"], p["predicted_quantity"]])

    report = {}
    for scenario, (key, result, demand30, raw, inv) in heroes.items():
        report[scenario] = {
            "product": key[0],
            "warehouse": key[1],
            "horizon": 30,
            "history_rows": len(history[key]),
            "forecast30_sum": demand30,
            "current_stock": inv["quantity"],
            "min_stock": inv["min_stock"],
            "raw_suggestion": raw,
            "smape": result["smape"],
            "mae": result["mae"],
            "rmse": result["rmse"],
        }
    (OUT / "hero-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
