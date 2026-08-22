# -*- coding: utf-8 -*-
"""
generate_official_v45_sql_3.py
Bản sao lưu script sinh dữ liệu V45 v3 theo yêu cầu của Prompt Master.
"""
from generate_official_v45_sql import *

if __name__ == "__main__":
    print("[INFO] Đang sinh file SQL V45 từ generate_official_v45_sql_3.py...")
    sql_text = build_sql()
    with open(OUTPUT_SQL_PATH, "w", encoding="utf-8") as f:
        f.write(sql_text)
    print(f"[SUCCESS] Đã sinh thành công {OUTPUT_SQL_PATH}")
    print(f"          Tổng số dòng SQL: {len(sql_text.splitlines()):,}")
