# Forecast Data Sources

All forecast history sources live together in `ai.lich_su_ban_hang` and are separated by exact `nguon_du_lieu`.

## Sources

- `EXTERNAL_STORE_ITEM`: external Store-Item benchmark source for research/evaluation.
- `SEED_DEMO`: StockSense-generated synthetic history for application demos and functional testing.
- `THUC_TE`: actual StockSense `XUAT_KHO` history for live operations.
- `EXTERNAL_RETAIL`: retired legacy source; keep count at 0 unless explicitly re-importing legacy data.

## Rule

One forecast run uses one exact source only.

Never merge histories across `EXTERNAL_STORE_ITEM`, `SEED_DEMO`, `THUC_TE`, or `EXTERNAL_RETAIL`.

When no source is supplied, backend defaults to `EXTERNAL_STORE_ITEM`.
