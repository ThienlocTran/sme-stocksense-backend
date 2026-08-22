import csv
import datetime as dt
import json
import math
import statistics
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
DATA = ROOT / "data_set"
OUT = ROOT / "worktrees" / "final-demo-data" / "target" / "final-demo-data"
START = dt.date(2023, 10, 1)
END = dt.date(2024, 9, 26)
STORES = ["1", "2", "3"]
DAYS = (END - START).days + 1


def dates():
    cur = START
    while cur <= END:
        yield cur
        cur += dt.timedelta(days=1)


def percentile(values, pct):
    if not values:
        return 0
    ordered = sorted(values)
    idx = min(len(ordered) - 1, math.ceil((pct / 100) * len(ordered)) - 1)
    return ordered[idx]


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    qty = defaultdict(lambda: defaultdict(int))
    totals = defaultdict(lambda: defaultdict(float))
    price_qty = defaultdict(lambda: defaultdict(float))
    source_rows = 0
    rejected = 0
    negative = 0

    with (DATA / "sales.csv").open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            source_rows += 1
            try:
                day = dt.date.fromisoformat(row["date"])
                store = row["store_id"]
                item = row["item_id"]
                q = float(row["quantity"])
                total = float(row["sum_total"])
                price = float(row["price_base"])
            except Exception:
                rejected += 1
                continue
            if store not in STORES or day < START or day > END or q < 0:
                if q < 0:
                    negative += 1
                continue
            key = (item, store)
            qty[key][day] += int(q)
            if q > 0 and total > 0 and price >= 0:
                totals[key][day] += total
                price_qty[key][day] += q

    items = sorted({item for item, store in qty})
    profiles = []
    for item in items:
        per_store = []
        all_values = []
        all_price_days = 0
        for store in STORES:
            values = [qty[(item, store)].get(day, 0) for day in dates()]
            all_values.extend(values)
            nz = sum(1 for v in values if v > 0)
            all_price_days += sum(1 for day in dates() if price_qty[(item, store)].get(day, 0) > 0)
            per_store.append({
                "store": store,
                "nonzero_ratio": nz / DAYS,
                "recent30": sum(values[-30:]),
            })
        mean = statistics.mean(all_values)
        median = statistics.median(all_values)
        p95 = percentile(all_values, 95)
        max_qty = max(all_values) if all_values else 0
        nonzero_ratio = sum(1 for v in all_values if v > 0) / len(all_values)
        recent30 = sum(
            qty[(item, store)].get(day, 0)
            for store in STORES
            for day in list(dates())[-30:]
        )
        spike_ratio = max_qty / max(1, p95)
        price_coverage = all_price_days / (DAYS * len(STORES))
        min_store_ratio = min(s["nonzero_ratio"] for s in per_store)
        accepted = (
            mean >= 0.15
            and mean <= 35
            and p95 <= 120
            and max_qty <= 500
            and nonzero_ratio >= 0.04
            and min_store_ratio >= 0.015
            and spike_ratio <= 15
            and recent30 > 0
        )
        score = (
            min(nonzero_ratio, 0.45) * 100
            + min(price_coverage, 0.5) * 30
            + min(mean, 12) * 3
            - max(0, spike_ratio - 6) * 4
            - max(0, p95 - 60) * 0.5
        )
        profiles.append({
            "item": item,
            "mean": mean,
            "median": median,
            "p95": p95,
            "max": max_qty,
            "nonzero_ratio": nonzero_ratio,
            "min_store_ratio": min_store_ratio,
            "recent30": recent30,
            "price_coverage": price_coverage,
            "spike_ratio": spike_ratio,
            "accepted": accepted,
            "score": score,
        })

    selected = sorted(
        [p for p in profiles if p["accepted"]],
        key=lambda p: (-p["score"], p["item"]),
    )[:100]
    if len(selected) != 100:
        raise SystemExit(f"Only {len(selected)} acceptable items")

    with (OUT / "external-retail-mapping-v2.csv").open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["type", "external_id", "stocksense_code", "rank", "metadata"])
        for i, store in enumerate(STORES, 1):
            w.writerow(["STORE", store, f"K{i:03d}", i, f"benchmark store {store}"])
        for i, p in enumerate(selected, 1):
            w.writerow(["ITEM", p["item"], f"SP{i:03d}", i, f"mean={p['mean']:.2f};p95={p['p95']};nz={p['nonzero_ratio']:.3f}"])

    selected_items = {p["item"]: i for i, p in enumerate(selected, 1)}
    with (OUT / "external-retail-history.csv").open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["product_code", "warehouse_code", "date", "quantity", "average_price", "source_reference"])
        for item, idx in selected_items.items():
            for si, store in enumerate(STORES, 1):
                for day in dates():
                    q = qty[(item, store)].get(day, 0)
                    pq = price_qty[(item, store)].get(day, 0)
                    avg = "" if pq <= 0 else f"{totals[(item, store)][day] / pq:.2f}"
                    w.writerow([f"SP{idx:03d}", f"K{si:03d}", day.isoformat(), q, avg, f"EXTERNAL_RETAIL:{store}:{item}"])

    with (OUT / "demo-inventory.csv").open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["product_code", "warehouse_code", "quantity", "min_stock", "scenario", "avg_daily"])
        scenarios = ["LOW"] * 36 + ["NEAR_RISK"] * 72 + ["HEALTHY"] * 180 + ["OVERSTOCK"] * 12
        n = 0
        for item, idx in selected_items.items():
            for si, store in enumerate(STORES, 1):
                values = [qty[(item, store)].get(day, 0) for day in dates()]
                avg = max(0.2, statistics.mean(values[-90:]))
                scenario = scenarios[n]
                if scenario == "LOW":
                    cover = 5
                elif scenario == "NEAR_RISK":
                    cover = 14
                elif scenario == "OVERSTOCK":
                    cover = 70
                else:
                    cover = 38
                min_stock = max(3, round(avg * 9))
                quantity = max(1, round(avg * cover))
                if scenario == "LOW":
                    quantity = max(1, min(quantity, max(1, min_stock - 1)))
                elif scenario == "NEAR_RISK":
                    quantity = max(min_stock + 1, quantity)
                elif scenario == "HEALTHY":
                    quantity = max(min_stock + 10, quantity)
                else:
                    quantity = max(min_stock + 50, quantity)
                w.writerow([f"SP{idx:03d}", f"K{si:03d}", quantity, min_stock, scenario, f"{avg:.4f}"])
                n += 1

    report = {
        "source_rows": source_rows,
        "rejected_rows": rejected,
        "negative_rows": negative,
        "candidates_inspected": len(profiles),
        "accepted_candidates": sum(1 for p in profiles if p["accepted"]),
        "selected_count": len(selected),
        "date_range": [START.isoformat(), END.isoformat()],
        "rows_to_import": len(selected) * len(STORES) * DAYS,
        "selected_summary": {
            "mean_daily_avg": statistics.mean(p["mean"] for p in selected),
            "median_daily_avg": statistics.median(p["median"] for p in selected),
            "p95_avg": statistics.mean(p["p95"] for p in selected),
            "max_qty": max(p["max"] for p in selected),
            "nonzero_ratio_avg": statistics.mean(p["nonzero_ratio"] for p in selected),
            "price_coverage_avg": statistics.mean(p["price_coverage"] for p in selected),
        },
        "selected_items": selected,
        "rejected_examples": sorted([p for p in profiles if not p["accepted"]], key=lambda p: (-p["max"], p["item"]))[:20],
    }
    (OUT / "profile-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({k: report[k] for k in ["candidates_inspected", "accepted_candidates", "selected_count", "rows_to_import", "selected_summary"]}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
