# Excel Import Template DB Audit

## 1. Current schema summary

- `san_pham`: đã có `ma_san_pham`, `ten_san_pham`, `sku`, `ma_vach`, `don_vi_tinh`, `danh_muc_id`, `ton_toi_thieu`, `ton_toi_da`, `price`, `trang_thai`.
- `danh_muc`: đã có `ma_danh_muc`, `ten_danh_muc`, `mo_ta`, `trang_thai`.
- `kho`: đã có `ma_kho`, `ten_kho`, `dia_chi`, `trang_thai`.
- `ton_kho`: tồn hiện tại theo cặp `san_pham_id` + `kho_id`.
- `giao_dich_kho`: lịch sử biến động tồn, có `loai_giao_dich`, `so_luong`, `so_luong_truoc`, `so_luong_sau`, `lan_import_id`.
- `lan_import_excel`: batch import, có `ten_file`, `duong_dan_file`, `loai_import`, `kho_id`, `trang_thai`, `tong_so_dong`, `so_dong_hop_le`, `so_dong_loi`.
- `loi_import_excel`: lỗi theo dòng import, có `lan_import_id`, `so_dong`, `ten_cot`, `gia_tri_goc`, `noi_dung_loi`, `goi_y_sua`.

DB runtime check: **skipped**. Không xác nhận được local DB connection an toàn trong ngữ cảnh này, nên audit dựa trên source + migration.

## 2. Existing entities/repositories/services

### Entity

- `Product` -> `san_pham`
- `Category` -> `danh_muc`
- `Warehouse` -> `kho`
- `InventoryLevel` -> `ton_kho`
- `InventoryTransaction` -> `giao_dich_kho`
- `ExcelImport` -> `lan_import_excel`
- `ExcelImportError` -> `loi_import_excel`

### Repository

- `ProductRepository`
- `CategoryRepository`
- `WarehouseRepository`
- `InventoryLevelRepository`
- `InventoryTransactionRepository`
- `ExcelImportRepository`
- `ExcelImportErrorRepository`

### Service

- `ProductService`
- `CategoryService`
- `WarehouseService`
- `InventoryService`
- `InventoryTransactionService`
- `ImportReceiptService`

Nhận xét: hiện có repo/entity cho import batch + error, nhưng chưa thấy controller/service riêng cho Excel import MVP.

## 3. Planned template summary

Sheets planned:

- `00_HuongDan`
- `01_SanPham`
- `02_TonDauKy`
- `03_DanhMuc_ThamChieu`
- `04_Kho_ThamChieu`
- `05_GiaTri_HopLe`
- `06_QuyTac_KiemTra`
- `07_Source_Research`

Business input sheets:

- `01_SanPham`
- `02_TonDauKy`

Reference sheets:

- `03_DanhMuc_ThamChieu`
- `04_Kho_ThamChieu`
- `05_GiaTri_HopLe`

## 4. Product sheet compatibility matrix

| Planned column | Direct in DB/entity? | Mapping decision | MVP | Required? | Note |
|---|---|---|---|---|---|
| `ma_san_pham` | Có, `san_pham.ma_san_pham` | Direct | Giữ | Có | Business key ổn định, unique |
| `ten_san_pham` | Có, `san_pham.ten_san_pham` | Direct | Giữ | Có | Khớp entity |
| `sku` | Có, `san_pham.sku` | Direct | Giữ | Không | Unique, optional |
| `ma_vach` | Có, `san_pham.ma_vach` | Direct | Giữ | Không | Unique, optional |
| `don_vi_tinh` | Có, `san_pham.don_vi_tinh` | Direct | Giữ | Có | Khớp entity |
| `ma_danh_muc` | Có gián tiếp qua `Category.code` + `Product.category` | Map `ma_danh_muc` -> `danh_muc.id` | Giữ | Theo template: Có | DB không lưu code trực tiếp trên product, nhưng reference sheet có thể tra được |
| `gia_nhap` | Không có cột đích trực tiếp | Chưa map an toàn | Deferred | Không nên giữ cho MVP nếu chưa có model giá nhập | Schema hiện chỉ có `price` |
| `gia_ban` | Có thể map vào `san_pham.price` | Map với cảnh báo semantics | Giữ có điều kiện | Không | Chỉ có 1 cột giá, không tách nhập/bán |
| `ton_toi_thieu` | Có, `san_pham.ton_toi_thieu` | Direct | Giữ | Không | Entity default `0` |
| `ton_toi_da` | Có, `san_pham.ton_toi_da` | Direct | Giữ | Không | Optional |
| `trang_thai` | Có, `san_pham.trang_thai` | Direct | Giữ | Không | Enum `HOAT_DONG`/`NGUNG_HOAT_DONG` |
| `ghi_chu` | Không có cột đích trực tiếp | Không map | Deferred | Không | Nếu cần note, phải thêm model riêng |

Kết luận `01_SanPham`: template đang **khớp phần lõi**, nhưng `gia_nhap` và `ghi_chu` là phần lệch model; `gia_ban` chỉ nên coi là `price`.

## 5. Opening stock sheet compatibility matrix

| Planned column | Direct in DB/entity? | Mapping decision | MVP | Required? | Note |
|---|---|---|---|---|---|
| `ma_kho` | Có gián tiếp qua `Warehouse.code` | Map `ma_kho` -> `kho.id` | Giữ | Có | Business code ổn định |
| `ma_san_pham` | Có gián tiếp qua `Product.code` | Map `ma_san_pham` -> `san_pham.id` | Giữ | Có | Business code ổn định |
| `so_luong_ton` | Có, `ton_kho.so_luong` | Direct | Giữ | Có | Số lượng tồn hiện tại |
| `don_gia_nhap` | Không có cột đích trực tiếp | Chưa map an toàn | Deferred | Không | Hiện chưa có model giá nhập cho tồn đầu kỳ |
| `ngay_ghi_nhan` | Không có cột đích trực tiếp | Có thể suy ra từ batch/transaction later | Deferred | Không | Có thể dùng ngày tạo import sau này, nhưng không phải schema hiện tại |
| `ghi_chu` | Không có cột đích trực tiếp | Không map | Deferred | Không | Chỉ nên dùng nếu thêm note staging |

Kết luận `02_TonDauKy`: chỉ `ma_kho`, `ma_san_pham`, `so_luong_ton` là khớp trực tiếp theo schema hiện tại.

## 6. Reference sheet feasibility

### `03_DanhMuc_ThamChieu`

Khả thi. `danh_muc` có `ma_danh_muc` stable, unique, và `trang_thai`.

### `04_Kho_ThamChieu`

Khả thi. `kho` có `ma_kho` stable, unique, và `trang_thai`.

### `05_GiaTri_HopLe`

Khả thi một phần. Có thể sinh từ source hiện tại:

- Product status: `HOAT_DONG`, `NGUNG_HOAT_DONG`
- Category status: `HOAT_DONG`, `NGUNG_HOAT_DONG`
- Warehouse status: `HOAT_DONG`, `NGUNG_HOAT_DONG`
- Import status: `CHO_XU_LY`, `CO_LOI`, `SAN_SANG_IMPORT`, `DA_IMPORT`, `THAT_BAI`
- Inventory transaction type: `NHAP_KHO`, `XUAT_KHO`, `NHAP_DAU_KY`, `DIEU_CHINH_TANG`, `DIEU_CHINH_GIAM`

Rủi ro: `PRODUCT_ONLY` và `PRODUCT_WITH_OPENING_STOCK` chưa có enum/constant riêng trong schema, nên nếu muốn show ở sheet thì phải coi là giá trị template nội bộ, không phải DB value.

## 7. MVP mode decision

### `PRODUCT_ONLY`

- Decision: **PARTIAL**
- Why: core product fields có thật, code-based refs có thật, nhưng template đang mang thêm `gia_nhap` và `ghi_chu` không có đích lưu rõ ràng. `gia_ban` chỉ map được vào `price`.

### `PRODUCT_WITH_OPENING_STOCK`

- Decision: **BLOCKED**
- Why: ngoài lệch field như trên, còn có `don_gia_nhap` và `ngay_ghi_nhan` chưa có đích trực tiếp; quan trọng hơn, flow tăng tồn và tạo `giao_dich_kho` cho import đầu kỳ được ghi nhận là để T163, không phải lúc này.

## 8. Risks/blockers

- Thiếu model đích cho `gia_nhap`, `don_gia_nhap`, `ngay_ghi_nhan`, `ghi_chu`.
- `san_pham` chỉ có 1 cột giá là `price`, không tách giá nhập/giá bán.
- Chưa thấy import staging/detail entity cho từng dòng Excel; hiện chỉ có batch `lan_import_excel` và error `loi_import_excel`.
- `PRODUCT_WITH_OPENING_STOCK` chưa thể chạm tồn kho thật ở giai đoạn này.
- DB runtime check chưa chạy, nên chưa xác nhận trực tiếp dữ liệu live.

## 9. Recommendation for T155/T156

- Giữ `ma_san_pham`, `ten_san_pham`, `don_vi_tinh`, `ma_danh_muc` làm lõi import product.
- Đổi `gia_ban` thành semantic rõ ràng hơn nếu chỉ có 1 cột giá; đừng song song giữ `gia_nhap` trừ khi thêm model riêng.
- Cân nhắc bỏ `ghi_chu` khỏi MVP template hoặc chấp nhận là cột tài liệu, không lưu DB.
- Với tồn đầu kỳ, chỉ nên chuẩn bị staging/import preview, chưa sinh transaction hay cập nhật `ton_kho` ở T155/T156.

## 10. Confirmation that Sprint 2 inbound workflow was not changed

Không thay đổi `ImportReceipt`, `ImportReceiptDetail`, `ImportReceiptServiceImpl`, approval flow, inventory transaction flow, stock update flow.
