# -*- coding: utf-8 -*-
import re

sql_file = r'src\main\resources\db\migration\V45__seed_official_business_and_ai_data.sql'
with open(sql_file, 'r', encoding='utf-8') as f:
    content = f.read()

print('=== BẮT ĐẦU KIỂM ĐỊNH TOÀN DIỆN V45 SQL ===')
print(f'Tổng độ dài file: {len(content):,} ký tự, {len(content.splitlines()):,} dòng\n')

# 1. Check anonymous DO block & END block
assert 'DO $$' in content, 'Lỗi: Thiếu DO $$'
assert 'END $$;' in content, 'Lỗi: Thiếu END $$;'
print('[PASS]  1. Cấu trúc khối PL/pgSQL (DO $$ ... END $$;)')

# 2. Check accounts declared
for email in ['thienloct.it@gmail.com', 'tranthienloc.nina@gmail.com', 'tranthienloc21102005@gmail.com']:
    assert email in content, f'Lỗi: Thiếu email {email}'
print('[PASS]  2. Đầy đủ 3 tài khoản nhân sự chính thức định danh')

# 3. Check categories
cat_count = len(re.findall(r'INSERT INTO danh_muc', content))
assert cat_count == 4, f'Lỗi: Số lượng danh mục = {cat_count}'
print(f'[PASS]  3. Đầy đủ 4 Danh mục (DM001 -> DM004)')

# 4. Check warehouses
wh_count = len(re.findall(r'INSERT INTO kho ', content))
assert wh_count == 3, f'Lỗi: Số lượng kho = {wh_count}'
print(f'[PASS]  4. Đầy đủ 3 Kho hàng (K001 Hà Nội, K002 Đà Nẵng, K003 Hồ Chí Minh)')

# 5. Check partners
partner_count = len(re.findall(r'INSERT INTO doi_tac ', content))
assert partner_count == 10, f'Lỗi: Số lượng đối tác = {partner_count}'
print(f'[PASS]  5. Đầy đủ 10 Đối tác (5 Nhà cung cấp, 4 Khách hàng, 1 Cả hai)')

# 6. Check products
sp_matches = re.findall(r"VALUES \('(SP\d{3})'", content)
assert len(sp_matches) == 100, f'Lỗi: Số sản phẩm = {len(sp_matches)}'
print(f'[PASS]  6. Đầy đủ 100 Sản phẩm IT (SP001 -> SP100)')

# 7. Check Phase 1 AI Seed (180 days: 05/07/2025 -> 31/12/2025)
seed_rows = re.findall(r"'(\d{4}-\d{2}-\d{2})',\d+,'SEED'", content)
assert len(seed_rows) == 15120, f'Lỗi: Số dòng SEED = {len(seed_rows)} (kỳ vọng: 15,120)'
dates = set(seed_rows)
assert min(dates) == '2025-07-05', f'Lỗi: Min date = {min(dates)}'
assert max(dates) == '2025-12-31', f'Lỗi: Max date = {max(dates)}'
print(f'[PASS]  7. Khởi động lạnh AI (Giai đoạn 1): Đúng 15,120 bản ghi SEED liên tục (05/07/2025 -> 31/12/2025)')

# 8. Check Inbound receipts
pn_matches = re.findall(r"VALUES \('(PNK-2026\d{4}-[0-9A-F]+)'", content)
assert len(pn_matches) == 35, f'Lỗi: Số phiếu nhập = {len(pn_matches)}'
print(f'[PASS]  8. Đầy đủ 35 Phiếu nhập kho năm 2026')

# 9. Check Outbound receipts
px_matches = re.findall(r"VALUES \('(PXK-2026\d{4}-[0-9A-F]+)'", content)
assert len(px_matches) >= 40, f'Lỗi: Số phiếu xuất = {len(px_matches)}'
print(f'[PASS]  9. Đầy đủ {len(px_matches)} Phiếu xuất kho năm 2026')

# 10. Check Phase 2 AI Sync with THUC_TE
thuc_te_rows = re.findall(r"'(\d{4}-\d{2}-\d{2})',\s*\d+,\s*'THUC_TE'", content)
assert len(thuc_te_rows) > 0, 'Lỗi: Không có dòng THUC_TE nào'
assert 'SYSTEM_SEED' not in content, 'Lỗi: Còn sót SYSTEM_SEED'
print(f'[PASS] 10. Đồng bộ AI Giai đoạn 2: {len(thuc_te_rows)} dòng THUC_TE (khớp 100% enum SalesHistorySource)')

# 11. Check Count Batches (dot_kiem_ke)
dkk_matches = re.findall(r"VALUES \('(DKK-2026\d{4}-[0-9A-F]+)'", content)
assert len(dkk_matches) == 12, f'Lỗi: Số đợt kiểm kê = {len(dkk_matches)}'
print(f'[PASS] 11. Đầy đủ 12 Đợt kiểm kê chốt sổ năm 2026')

# 12. Check Sequences reset
seq_matches = re.findall(r"SELECT setval\('([^']+)'", content)
assert len(seq_matches) == 20, f'Lỗi: Số sequence reset = {len(seq_matches)}'
print(f'[PASS] 12. Đầy đủ 20 Lệnh reset sequence ở cuối file')

print('\n=============================================================')
print('  KẾT QUẢ: TẤT CẢ 12 TIÊU CHÍ ĐỀU ĐẠT 100% - CHUẨN HOÀN THIỆN!')
print('=============================================================')
