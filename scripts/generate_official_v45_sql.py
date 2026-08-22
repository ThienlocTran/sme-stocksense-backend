# -*- coding: utf-8 -*-
# NOTE: All SQL string literals with Vietnamese MUST use f-strings carefully.
# The file is written with encoding='utf-8' explicitly.
"""
generate_official_v45_sql.py — Master v5.0 (Prompt Master Edition)
=======================================================================
- Giai đoạn 1 (2025): 180 ngày SEED (05/07 → 31/12/2025)
  → Chỉ INSERT ai.lich_su_ban_hang nguon='SEED', KHÔNG phiếu nhập/xuất
- Giai đoạn 2 (2026): 01/01 → 21/08/2026
  → Phiếu Nhập/Xuất thực tế
  → Phiếu Xuất HOAN_THANH → INSERT ai.lich_su_ban_hang nguon='THUC_TE'
  → Padding mọi ngày không có xuất → INSERT qty=0 nguon='THUC_TE'
- Inventory Health: ~5% hết, ~15% sắp hết, ~80% còn hàng
- Entity-Aligned: 100% tên cột bám sát Java Entity
=======================================================================
"""

import os, random, calendar, datetime, hashlib
from typing import List, Tuple, Dict

OUTPUT_SQL_PATH = os.path.join(
    os.path.dirname(__file__), "..", "src", "main", "resources", "db", "migration",
    "V45__seed_official_business_and_ai_data.sql"
)

# ── 100 SẢN PHẨM IT ──────────────────────────────────────────────────
# (ma_sp, ten_sp, sku, don_vi, ma_dm, ma_ncc, the_tich_m3, ton_min, gia_xuat)
PRODUCTS: List[Tuple] = [
    # DM001 - Điện tử
    ("SP001","Laptop Dell XPS 13 Plus 9320 Core i7-1360P 16GB 512GB 13.4 inch OLED",         "VN-DELL-XPS9320",    "Cái","DM001","DT-NCC01",0.0032, 5,41500000),
    ("SP002","Laptop ASUS Zenbook 14 OLED UX3405MA Core Ultra 7 155H 32GB 1TB 14 inch 3K",   "VN-ASUS-UX3405MA",   "Cái","DM001","DT-NCC03",0.0031, 6,31900000),
    ("SP003","Laptop Lenovo ThinkPad X1 Carbon Gen 11 Core i7-1355U 16GB 512GB 14 inch",      "VN-LNV-X1CARB11",    "Cái","DM001","DT-NCC02",0.0033, 4,38500000),
    ("SP004","Laptop HP Spectre x360 14-ef2003TU Core i7-1355U 16GB 1TB 13.5 inch OLED",     "VN-HP-SPEC14X360",   "Cái","DM001","DT-NCC02",0.0034, 4,36900000),
    ("SP005","Laptop Acer Swift Go 14 SFG14-73 Core Ultra 7 155H 16GB 512GB OLED",           "VN-ACR-SFG1473",     "Cái","DM001","DT-NCC03",0.0030, 8,22900000),
    ("SP006","Laptop Gaming ASUS ROG Zephyrus G16 GU605MI Core Ultra 9 RTX 4070 OLED",       "VN-ASUS-G16GU605",   "Cái","DM001","DT-NCC03",0.0048, 3,58900000),
    ("SP007","Laptop Gaming Lenovo Legion Pro 5 16IRX9 i7-14650HX RTX 4060 16GB 512GB",      "VN-LNV-LEGPRO5",     "Cái","DM001","DT-NCC02",0.0052, 5,37500000),
    ("SP008","Laptop Gaming Acer Predator Helios Neo 16 PHN16-72 i7-14700HX RTX 4060",       "VN-ACR-PHN1672",     "Cái","DM001","DT-NCC03",0.0055, 5,35900000),
    ("SP009","Laptop Gaming MSI Katana 15 B13VEK Core i7-13620H RTX 4050 16GB 1TB",          "VN-MSI-KAT15B13",    "Cái","DM001","DT-NCC03",0.0050, 6,24500000),
    ("SP010","Laptop Gaming Dell G15 5530 Core i7-13650HX RTX 4050 16GB 512GB 165Hz",        "VN-DELL-G155530",    "Cái","DM001","DT-NCC01",0.0054, 5,27900000),
    ("SP011","Máy tính để bàn Apple Mac mini M2 8-core CPU 10-core GPU 8GB 256GB SSD",       "VN-APL-MACMINIM2",   "Bộ","DM001","DT-NCC01",0.0022, 6,14500000),
    ("SP012","Máy tính để bàn Apple Mac Studio M2 Max 12-core CPU 30-core GPU 32GB 512GB",   "VN-APL-MACSTUM2",    "Bộ","DM001","DT-NCC01",0.0085, 2,52900000),
    ("SP013","Máy tính để bàn HP Pro Mini 400 G9 i5-13500T 8GB 256GB SSD Windows 11 Pro",   "VN-HP-PRO400G9",     "Bộ","DM001","DT-NCC02",0.0035, 8,13800000),
    ("SP014","Máy tính để bàn ASUS ROG Strix GT15 i7-13700F RTX 4070 32GB 1TB SSD",         "VN-ASUS-GT154070",   "Bộ","DM001","DT-NCC03",0.0350, 3,44500000),
    ("SP015","Màn hình Dell UltraSharp U2723QE 27 inch 4K IPS Type-C 90W",                   "VN-DELL-U2723QE",    "Cái","DM001","DT-NCC01",0.0450, 6,13500000),
    ("SP016","Màn hình LG 27UP850N-W 27 inch 4K UHD IPS HDR400 Type-C 96W",                 "VN-LG-27UP850N",     "Cái","DM001","DT-NCC02",0.0420, 6, 8900000),
    ("SP017","Màn hình đồ họa ASUS ProArt PA278CV 27 inch 2K IPS 75Hz 100% sRGB",           "VN-ASUS-PA278CV",    "Cái","DM001","DT-NCC03",0.0410, 5, 8600000),
    ("SP018","Màn hình Gaming Samsung Odyssey G7 G70B 28 inch 4K UHD IPS 144Hz",             "VN-SAM-G70B28",      "Cái","DM001","DT-NCC05",0.0480, 4,12800000),
    ("SP019","Màn hình chuyên nghiệp ViewSonic VP2768a 27 inch 2K IPS ColorPro",             "VN-VS-VP2768A",      "Cái","DM001","DT-NCC02",0.0400, 4, 7800000),
    ("SP020","Router Wi-Fi 6 TP-Link Archer AX73 Băng tần kép AX5400",                       "VN-TPL-AX73",        "Cái","DM001","DT-NCC04",0.0065,12, 2450000),
    ("SP021","Router Wi-Fi 6 ASUS RT-AX88U Pro Băng tần kép AX6000",                         "VN-ASUS-AX88UPRO",   "Cái","DM001","DT-NCC03",0.0085, 8, 6800000),
    ("SP022","Router MikroTik hEX RB750Gr3 5 Cổng Gigabit Cân bằng tải đa WAN",             "VN-MTK-RB750GR3",    "Cái","DM001","DT-NCC04",0.0015,15, 1350000),
    ("SP023","Router cân bằng tải DrayTek Vigor 2927 Dual-WAN Gigabit Tường lửa",            "VN-DRK-VIGOR2927",   "Cái","DM001","DT-NCC04",0.0038, 8, 4250000),
    ("SP024","Bộ phát Wi-Fi doanh nghiệp Ubiquiti UniFi U6-Pro Wi-Fi 6 gắn trần",            "VN-UBNT-U6PRO",      "Cái","DM001","DT-NCC04",0.0032,10, 3850000),
    ("SP025","Switch Cisco Business CBS110-16T 16 Cổng Gigabit Không quản lý",               "VN-CSC-CBS11016T",   "Cái","DM001","DT-NCC04",0.0055, 6, 2650000),
    # DM002 - Linh kiện
    ("SP026","RAM Desktop Kingston Fury Beast 16GB DDR4 3200MHz Tản nhiệt thép",              "VN-KST-FURY-16G4",   "Thanh","DM002","DT-NCC01",0.0003,20,  950000),
    ("SP027","RAM Desktop Corsair Vengeance RGB PRO 32GB 2x16GB DDR4 3600MHz",               "VN-COR-VGBRGB-32G4","Bộ","DM002","DT-NCC02",0.0006,10, 2250000),
    ("SP028","RAM Desktop G.Skill Trident Z5 RGB 32GB 2x16GB DDR5 6000MHz",                  "VN-GSK-TZ5-32G5",    "Bộ","DM002","DT-NCC03",0.0006, 8, 3450000),
    ("SP029","RAM Laptop Crucial 16GB DDR4 3200MHz SODIMM 260-Pin",                          "VN-CRC-NB16G4",      "Thanh","DM002","DT-NCC01",0.0002,15,  890000),
    ("SP030","RAM Laptop Kingston Fury Impact 16GB DDR5 4800MHz SODIMM",                     "VN-KST-FI16G5",      "Thanh","DM002","DT-NCC01",0.0002,12, 1350000),
    ("SP031","Ổ cứng SSD Samsung 980 PRO 1TB PCIe NVMe M.2 Đọc 7000MB/s",                  "VN-SAM-980PRO-1T",   "Cái","DM002","DT-NCC02",0.0003,15, 2690000),
    ("SP032","Ổ cứng SSD Kingston NV2 1TB PCIe 4.0 NVMe M.2 Đọc 3500MB/s",                 "VN-KST-NV2-1T",      "Cái","DM002","DT-NCC01",0.0003,25, 1490000),
    ("SP033","Ổ cứng SSD WD Black SN850X 1TB PCIe Gen4 NVMe Đọc 7300MB/s",                  "VN-WDC-SN850X-1T",   "Cái","DM002","DT-NCC02",0.0003,10, 2590000),
    ("SP034","Ổ cứng SSD Crucial P3 Plus 500GB M.2 PCIe Gen4 NVMe Đọc 5000MB/s",            "VN-CRC-P3P-500G",    "Cái","DM002","DT-NCC01",0.0003,18,  980000),
    ("SP035","Ổ cứng SSD Kioxia Exceria G2 1TB M.2 2280 NVMe Đọc 2100MB/s",                 "VN-KXA-EXCG2-1T",    "Cái","DM002","DT-NCC03",0.0003,12, 1390000),
    ("SP036","Bàn phím cơ không dây Keychron K2 Pro QMK VIA RGB Red Switch Hotswap",         "VN-KCH-K2PRO-RD",    "Cái","DM002","DT-NCC02",0.0032,10, 2450000),
    ("SP037","Bàn phím cơ Akko 3087 v2 Steam Engine Orange Switch PBT Side-Printed",         "VN-AKK-3087V2-SE",   "Cái","DM002","DT-NCC03",0.0035,12, 1290000),
    ("SP038","Bàn phím văn phòng không dây Logitech MX Keys S Bluetooth Đèn nền thông minh", "VN-LOG-MXKEYS-S",    "Cái","DM002","DT-NCC02",0.0028,10, 2690000),
    ("SP039","Bàn phím cơ Gaming Corsair K70 RGB PRO Cherry MX Red Switch 8000Hz",           "VN-COR-K70PRO-RD",   "Cái","DM002","DT-NCC02",0.0048, 6, 3590000),
    ("SP040","Bàn phím cơ Gaming Razer Huntsman Mini 60% Optical Switch Purple",              "VN-RZR-HUNTMINI-PU", "Cái","DM002","DT-NCC03",0.0025, 6, 2190000),
    ("SP041","Chuột không dây Logitech MX Master 3S Dark Grey Cảm biến 8K DPI",              "VN-LOG-MXM3S-GRY",   "Cái","DM002","DT-NCC02",0.0016,15, 2190000),
    ("SP042","Chuột Gaming Logitech G502 HERO 25K Sensor Tùy chỉnh tạ trọng lượng",          "VN-LOG-G502HERO",    "Cái","DM002","DT-NCC02",0.0015,18,  990000),
    ("SP043","Chuột Gaming không dây Razer DeathAdder V3 Pro Wireless White 63g",            "VN-RZR-DAV3PRO-W",   "Cái","DM002","DT-NCC03",0.0014, 8, 3190000),
    ("SP044","Chuột công thái học Logitech Lift Vertical Ergonomic Không dây Hồng",          "VN-LOG-LIFT-RS",     "Cái","DM002","DT-NCC02",0.0017,10, 1390000),
    ("SP045","Chuột Gaming Fuhlen G90 Optical Pro Switch Magneto-Driven Bất tử",             "VN-FHL-G90PRO",      "Cái","DM002","DT-NCC03",0.0012,30,  350000),
    ("SP046","Webcam Logitech C922 Pro Stream Full HD 1080p 60fps Tự động lấy nét",          "VN-LOG-C922PRO",     "Cái","DM002","DT-NCC02",0.0011,10, 1950000),
    ("SP047","Webcam Razer Kiyo Pro HDR 1080p Cảm biến ánh sáng thích ứng góc rộng",        "VN-RZR-KIYOPRO",     "Cái","DM002","DT-NCC03",0.0018, 5, 2890000),
    ("SP048","Tai nghe Gaming HyperX Cloud II 7.1 Surround Sound Màu Đỏ Bọc nhung",         "VN-HPX-CLOUD2-RD",   "Cái","DM002","DT-NCC01",0.0055, 8, 1750000),
    ("SP049","Tai nghe Gaming Corsair HS55 Surround Lightweight Carbon Đệm mút",             "VN-COR-HS55SUR-CB",  "Cái","DM002","DT-NCC02",0.0048,10, 1290000),
    ("SP050","Tai nghe chống ồn Sony WH-1000XM4 Hi-Res Audio Wireless Màu Đen",             "VN-SNY-WH1000XM4",   "Cái","DM002","DT-NCC05",0.0058, 5, 5990000),
    # DM003 - Đồ dùng
    ("SP051","Cáp HDMI 2.1 8K Ugreen 70321 Dài 2m Bọc dù chống đứt gãy",                   "VN-UGR-HDMI21-2M",   "Sợi","DM003","DT-NCC02",0.0004,30,  240000),
    ("SP052","Cáp DisplayPort 1.4 8K Ugreen 80392 Dài 2m Đầu hợp kim nhôm",                "VN-UGR-DP14-2M",     "Sợi","DM003","DT-NCC02",0.0004,20,  270000),
    ("SP053","Cáp sạc nhanh Anker 322 Type-C to Type-C 60W Dài 0.9m Bọc Nylon",            "VN-ANK-322-CC09M",   "Sợi","DM003","DT-NCC03",0.0002,40,  140000),
    ("SP054","Cáp sạc nhanh Anker PowerLine III 100W Type-C Dài 1.8m",                      "VN-ANK-PL3-100W",    "Sợi","DM003","DT-NCC03",0.0003,25,  290000),
    ("SP055","Cáp sạc Baseus Dynamic Type-C to Lightning 20W Dài 1m Chuẩn MFi Apple",       "VN-BAS-DYN-CL1M",    "Sợi","DM003","DT-NCC03",0.0002,35,  120000),
    ("SP056","Cáp mạng Cat6 Ugreen 20161 UTP Dài 5m Dây dẹt luồn góc thông minh",          "VN-UGR-CAT6-5M",     "Sợi","DM003","DT-NCC02",0.0005,50,   80000),
    ("SP057","Cáp mạng Cat7 Ugreen 11263 STP Bọc kim loại chống nhiễu 10Gbps Dài 10m",      "VN-UGR-CAT7-10M",    "Sợi","DM003","DT-NCC02",0.0008,25,  210000),
    ("SP058","Củ sạc nhanh 3 cổng Anker 735 GaNPrime 65W Type-C và USB-A",                  "VN-ANK-735-65W",     "Cái","DM003","DT-NCC03",0.0004,15,  950000),
    ("SP059","Củ sạc nhanh 4 cổng Baseus GaN5 Pro 100W Type-C USB Sạc Laptop",              "VN-BAS-GAN5P-100W",  "Cái","DM003","DT-NCC03",0.0006,12,  890000),
    ("SP060","Củ sạc Apple 20W USB-C Power Adapter Chính hãng Apple Việt Nam",              "VN-APL-20W-USBC",    "Cái","DM003","DT-NCC01",0.0003,25,  490000),
    ("SP061","Hub Type-C 8 trong 1 Ugreen 15375 4K@60Hz HDMI PD100W LAN",                   "VN-UGR-HUB8IN1",     "Cái","DM003","DT-NCC02",0.0005,15,  890000),
    ("SP062","Hub Type-C 6 trong 1 Baseus Metal Gleam Vỏ nhôm nguyên khối",                 "VN-BAS-6IN1-MG",     "Cái","DM003","DT-NCC03",0.0004,20,  550000),
    ("SP063","Đầu đọc thẻ nhớ SD/TF USB 3.0 Type-C Ugreen 50706 Tốc độ cao",               "VN-UGR-CR127-SD",    "Cái","DM003","DT-NCC02",0.0002,30,  220000),
    ("SP064","USB 3.2 Gen1 Kingston DataTraveler Exodia M 64GB Nắp trượt",                  "VN-KST-DTXM-64G",    "Cái","DM003","DT-NCC01",0.0001,50,  120000),
    ("SP065","USB 3.0 SanDisk Ultra Flair CZ73 128GB Vỏ kim loại 150MB/s",                  "VN-SND-CZ73-128G",   "Cái","DM003","DT-NCC02",0.0001,35,  260000),
    ("SP066","Thẻ nhớ SanDisk Extreme microSDXC 128GB 190MB/s 4K UHD A2 V30",               "VN-SND-EXT-128G",    "Cái","DM003","DT-NCC02",0.0001,30,  380000),
    ("SP067","Hộp đựng ổ cứng 2.5 inch SATA USB 3.0 Orico 2588US3 Chống sốc",              "VN-ORI-2588US3",     "Cái","DM003","DT-NCC03",0.0005,25,  140000),
    ("SP068","Box M.2 NVMe PCIe sang USB-C 10Gbps Orico PWM2 Vỏ tản nhiệt nhôm",           "VN-ORI-PWM2-10G",    "Cái","DM003","DT-NCC03",0.0004,18,  380000),
    ("SP069","Giá đỡ Laptop hợp kim nhôm gập gọn xoay 360 độ Moft Z Stand",                "VN-MFT-ZALUM-360",   "Cái","DM003","DT-NCC02",0.0035,10,  890000),
    ("SP070","Giá treo màn hình Arm North Bayou F80 17-30 inch Tải 9kg Lò xo khí nén",      "VN-NB-F80-ARMGAS",   "Bộ","DM003","DT-NCC03",0.0075,15,  380000),
    ("SP071","Ổ cắm điện thông minh Xiaomi Power Strip 6 Cổng 3 USB Sạc nhanh",             "VN-XMI-STRIP6P",     "Cái","DM003","DT-NCC05",0.0018,20,  290000),
    ("SP072","Ổ cắm điện chống sét APC Essential SurgeArrest 5 Cổng 230V",                  "VN-APC-PM5VGR",      "Cái","DM003","DT-NCC04",0.0028,12,  590000),
    ("SP073","Túi chống sốc Laptop 14 inch Tomtoc Defender-A13 CornerArmor",                "VN-TMC-A13-14BK",    "Cái","DM003","DT-NCC02",0.0032,15,  680000),
    ("SP074","Bàn di chuột SteelSeries QcK Heavy XXL 900x400x4mm Vải dệt micro",            "VN-SS-QCKHEAVYXXL",  "Cái","DM003","DT-NCC03",0.0038,20,  790000),
    ("SP075","Bộ dụng cụ sửa chữa máy tính 128 món Nanch Hợp kim thép S2 cao cấp",          "VN-NCH-128IN1-KIT",  "Bộ","DM003","DT-NCC03",0.0016,10,  480000),
    # DM004 - Thiết bị
    ("SP076","Bộ nguồn Corsair RM850e 850W 80 Plus Gold ATX 3.0 PCIe 5.0",                  "VN-COR-RM850E",      "Cái","DM004","DT-NCC02",0.0075, 8, 3190000),
    ("SP077","Bộ nguồn Seasonic Focus GX-750 750W 80 Plus Gold Full Modular",               "VN-SEA-GX750",       "Cái","DM004","DT-NCC03",0.0072, 8, 2950000),
    ("SP078","Bộ nguồn Cooler Master MWE 650 Bronze V2 650W 80 Plus Bronze",                "VN-CM-MWE650BV2",    "Cái","DM004","DT-NCC03",0.0068,15, 1450000),
    ("SP079","Bộ nguồn MSI MAG A650BN 650W 80 Plus Bronze Quạt 120mm siêu êm",              "VN-MSI-A650BN",      "Cái","DM004","DT-NCC03",0.0065,15, 1290000),
    ("SP080","Bộ nguồn ASUS ROG Thor 1000W Platinum II Màn hình OLED",                      "VN-ASUS-THOR1000P",  "Cái","DM004","DT-NCC03",0.0125, 3, 9890000),
    ("SP081","Tản nhiệt CPU DeepCool AK620 Digital Màn hình nhiệt độ 6 Ống đồng",           "VN-DPC-AK620DIG",    "Bộ","DM004","DT-NCC03",0.0085,10, 1690000),
    ("SP082","Tản nhiệt CPU Thermalright Peerless Assassin 120 SE ARGB 6 Ống kép",           "VN-TR-PA120SE-ARGB", "Bộ","DM004","DT-NCC03",0.0082,15,  890000),
    ("SP083","Tản nhiệt nước AIO NZXT Kraken 240 RGB Màn hình LCD 1.54 inch Đen",           "VN-NZXT-KRAK240B",   "Bộ","DM004","DT-NCC02",0.0145, 6, 3850000),
    ("SP084","Tản nhiệt nước AIO Corsair iCUE LINK H150i RGB 360mm Quạt nam châm",           "VN-COR-H150ILNK",    "Bộ","DM004","DT-NCC02",0.0185, 5, 5490000),
    ("SP085","Tản nhiệt nước AIO Thermalright Aqua Elite 240 V3 ARGB Bơm thế hệ 4",         "VN-TR-AE240V3-ARGB", "Bộ","DM004","DT-NCC03",0.0135,10, 1290000),
    ("SP086","Quạt tản nhiệt Noctua NF-A12x25 PWM 120mm Ổ trục SSO2 Siêu êm",              "VN-NCT-NFA12X25",    "Cái","DM004","DT-NCC02",0.0009,20,  780000),
    ("SP087","Quạt tản nhiệt Arctic P12 PWM PST 120mm Áp suất tĩnh cao",                    "VN-ARC-P12PWMPST",   "Cái","DM004","DT-NCC03",0.0008,30,  210000),
    ("SP088","Bộ 3 Quạt Thermalright TL-C12C-S ARGB 120mm Đồng bộ màu",                    "VN-TR-TLC12CS-3P",   "Bộ","DM004","DT-NCC03",0.0028,20,  390000),
    ("SP089","Bộ 3 Quạt Lian Li UNI FAN SL-INFINITY 120 RGB Gương vô cực",                  "VN-LL-SLINF120-3P",  "Bộ","DM004","DT-NCC02",0.0035, 8, 2450000),
    ("SP090","Keo tản nhiệt Arctic MX-4 4g Dẫn nhiệt cao không dẫn điện",                   "VN-ARC-MX4-4G",      "Tuýp","DM004","DT-NCC03",0.0001,40,  140000),
    ("SP091","Keo tản nhiệt Thermal Grizzly Kryonaut 1g Hiệu năng ép xung 12.5 W/mk",       "VN-TG-KRYO-1G",      "Tuýp","DM004","DT-NCC02",0.0001,25,  190000),
    ("SP092","Vỏ case NZXT H5 Flow Compact ATX Mid-Tower Mặt lưới Đen",                     "VN-NZXT-H5FLOW-BK",  "Cái","DM004","DT-NCC02",0.0650, 6, 2190000),
    ("SP093","Vỏ case Montech AIR 903 MAX Đen Kèm 4 Quạt 140mm ARGB",                       "VN-MNT-AIR903M-BK",  "Cái","DM004","DT-NCC03",0.0750, 6, 1750000),
    ("SP094","Vỏ case Lian Li O11 Dynamic EVO Hai mặt kính cường lực ATX",                  "VN-LL-O11DEVO-BK",   "Cái","DM004","DT-NCC02",0.0880, 4, 3890000),
    ("SP095","Vỏ case ASUS Prime AP201 MicroATX Lưới thép tản nhiệt 33L",                   "VN-ASUS-AP201-BK",   "Cái","DM004","DT-NCC03",0.0480, 8, 1690000),
    ("SP096","Bộ lưu điện UPS Santak Offline TG500 500VA 300W Bảo vệ văn phòng",            "VN-STK-TG500-OFF",   "Cái","DM004","DT-NCC04",0.0085,10, 1050000),
    ("SP097","Bộ lưu điện UPS APC Easy UPS BVX1200LI-MS 1200VA 650W AVR",                   "VN-APC-BVX1200LI",   "Cái","DM004","DT-NCC04",0.0165, 5, 2450000),
    ("SP098","Bộ lưu điện UPS Santak Online C1K-LCD 1kVA 900W Sóng Sin chuẩn Server",       "VN-STK-C1KLCD-ON",   "Cái","DM004","DT-NCC04",0.0245, 3, 7890000),
    ("SP099","Dây nguồn bọc lưới Lian Li Strimer Plus V2 24 Pin ARGB Đồng bộ LED",         "VN-LL-STRIMER24P-V2","Bộ","DM004","DT-NCC02",0.0018,10, 1490000),
    ("SP100","Giá đỡ card đồ họa ASUS ROG Herculx RGB Đế nam châm chống xệ GPU",           "VN-ASUS-ROGHERC-RGB","Cái","DM004","DT-NCC03",0.0014,15,  990000),
]

WH_LIST  = ["K001","K002","K003"]
LOC_MULT = {"K001":1.10, "K002":0.75, "K003":1.35}

SUP_LIST  = ["DT-NCC01","DT-NCC02","DT-NCC03","DT-NCC04","DT-NCC05"]
CUST_LIST = ["DT-KH01","DT-KH02","DT-KH03","DT-KH04","DT-CH01"]

def h12(s):
    return hashlib.md5(s.encode()).hexdigest()[:12].upper()


def build_sql() -> str:
    L = []
    def w(s=""): L.append(s)

    w("-- ================================================================")
    w("-- V45__seed_official_business_and_ai_data.sql  (v5.0 Prompt Master)")
    w("-- ================================================================")
    w("")
    w("DO $$")
    w("DECLARE")
    w("    v_admin_id   BIGINT;")
    w("    v_manager_id BIGINT;")
    w("    v_emp_id     BIGINT;")
    w("    v_pn_id      BIGINT;")
    w("    v_px_id      BIGINT;")
    w("    v_dkk_id     BIGINT;")
    w("BEGIN")
    w("    SELECT id INTO v_admin_id   FROM nhan_vien WHERE email='thienloct.it@gmail.com';")
    w("    SELECT id INTO v_manager_id FROM nhan_vien WHERE email='tranthienloc.nina@gmail.com';")
    w("    SELECT id INTO v_emp_id     FROM nhan_vien WHERE email='tranthienloc21102005@gmail.com';")
    w("    IF v_admin_id IS NULL OR v_manager_id IS NULL OR v_emp_id IS NULL THEN")
    w("        RAISE EXCEPTION 'Khong tim thay du 3 tai khoan nhan su!';")
    w("    END IF;")
    w("")

    # ── DANH MỤC ─────────────────────────────────────────────────────
    w("    -- 1. DANH MỤC")
    for ma,ten,mo_ta in [
        ("DM001","Điện tử",  "Thiết bị điện tử máy tính, laptop, màn hình và mạng"),
        ("DM002","Linh kiện","Linh kiện phần cứng RAM, SSD, bàn phím, chuột"),
        ("DM003","Đồ dùng",  "Cáp sạc, phụ kiện chuyển đổi và đồ dùng văn phòng IT"),
        ("DM004","Thiết bị", "Nguồn máy tính, tản nhiệt, vỏ case và bộ lưu điện UPS"),
    ]:
        w(f"    INSERT INTO danh_muc (ma_danh_muc,ten_danh_muc,mo_ta,trang_thai,ngay_tao,ngay_cap_nhat)")
        w(f"    VALUES ('{ma}','{ten}','{mo_ta}','HOAT_DONG'::trang_thai_danh_muc,now(),now())")
        w(f"    ON CONFLICT (ma_danh_muc) DO UPDATE SET ten_danh_muc=EXCLUDED.ten_danh_muc,ngay_cap_nhat=now();")
    w("")

    # ── KHO ──────────────────────────────────────────────────────────
    w("    -- 2. KHO")
    for ma,ten,dc,cap in [
        ("K001","Kho Hà Nội",      "Số 1 Phạm Hùng, Mễ Trì, Nam Từ Liêm, Hà Nội",           2500.0),
        ("K002","Kho Đà Nẵng",     "Số 50 Nguyễn Văn Linh, Nam Dương, Hải Châu, Đà Nẵng",    1800.0),
        ("K003","Kho Hồ Chí Minh", "Số 285 Cách Mạng Tháng Tám, Phường 12, Quận 10, TP.HCM", 3000.0),
    ]:
        w(f"    INSERT INTO kho (ma_kho,ten_kho,dia_chi,suc_chua_toi_da_m3,trang_thai,ngay_tao,ngay_cap_nhat)")
        w(f"    VALUES ('{ma}','{ten}','{dc}',{cap:.3f},'HOAT_DONG'::trang_thai_kho,now(),now())")
        w(f"    ON CONFLICT (ma_kho) DO UPDATE SET ten_kho=EXCLUDED.ten_kho,suc_chua_toi_da_m3=EXCLUDED.suc_chua_toi_da_m3,ngay_cap_nhat=now();")
    w("")

    # ── ĐỐI TÁC ──────────────────────────────────────────────────────
    w("    -- 3. ĐỐI TÁC")
    PARTNERS = [
        ("DT-NCC01","Công ty Cổ phần Synnex FPT",                          "NHA_CUNG_CAP","02873001010","synnex.fpt@synnexfpt.com.vn",   "Tòa nhà FPT, 10 Phạm Văn Bạch, Cầu Giấy, Hà Nội"),
        ("DT-NCC02","Công ty Cổ phần Thế Giới Số Digiworld",               "NHA_CUNG_CAP","02839290059","contact@digiworld.com.vn",       "Tầng 16, 190 Pasteur, Quận 3, TP.HCM"),
        ("DT-NCC03","Công ty Cổ phần Thương mại Dịch vụ Phong Vũ",         "NHA_CUNG_CAP","18006867",   "b2b@phongvu.vn",                 "264 Nguyễn Thị Minh Khai, Quận 3, TP.HCM"),
        ("DT-NCC04","Tổng Công ty Cổ phần Xuất nhập khẩu Viettel Commerce", "NHA_CUNG_CAP","02462778899","b2b@viettelstore.vn",            "1 Giang Văn Minh, Ba Đình, Hà Nội"),
        ("DT-NCC05","Tổng Công ty Cổ phần Petrosetco",                      "NHA_CUNG_CAP","02839117777","contact@petrosetco.com.vn",      "Lầu 6, 1-5 Lê Duẩn, Quận 1, TP.HCM"),
        ("DT-KH01","Công ty TNHH Công nghệ Sao Việt",                       "KHACH_HANG",  "02838383838","contact@saoviet-tech.vn",        "45 Lê Duẩn, Bến Nghé, Quận 1, TP.HCM"),
        ("DT-KH02","Công ty TNHH Giải pháp Số Miền Trung",                  "KHACH_HANG",  "02363889999","info@mientrung-solutions.com",   "120 Nguyễn Thị Minh Khai, Hải Châu, Đà Nẵng"),
        ("DT-KH03","Công ty TNHH Thiết bị Văn phòng Minh Long",             "KHACH_HANG",  "02437668899","sales@minhlong-office.vn",       "88 Cầu Giấy, Quan Hoa, Cầu Giấy, Hà Nội"),
        ("DT-KH04","Công ty Cổ phần Bán lẻ Công nghệ Miền Bắc",            "KHACH_HANG",  "02439887766","b2b@mienbac-retail.vn",          "200 Trần Duy Hưng, Trung Hòa, Cầu Giấy, Hà Nội"),
        ("DT-CH01","Công ty TNHH Phân Phối Thế Giới Số Việt",               "CA_HAI",      "02838992233","support@vietnamdigi.vn",         "512 Lý Thường Kiệt, Tân Bình, TP.HCM"),
    ]
    for ma,ten,loai,sdt,email,dc in PARTNERS:
        w(f"    INSERT INTO doi_tac (ma_doi_tac,ten_doi_tac,loai_doi_tac,so_dien_thoai,email,dia_chi,trang_thai,ngay_tao,ngay_cap_nhat)")
        w(f"    VALUES ('{ma}','{ten}','{loai}'::loai_doi_tac,'{sdt}','{email}','{dc}','HOAT_DONG'::trang_thai_doi_tac,now(),now())")
        w(f"    ON CONFLICT (ma_doi_tac) DO UPDATE SET ten_doi_tac=EXCLUDED.ten_doi_tac,ngay_cap_nhat=now();")
    w("")

    # ── SẢN PHẨM ─────────────────────────────────────────────────────
    # Columns: ma_san_pham, ten_san_pham, sku, don_vi_tinh, danh_muc_id,
    #          doi_tac_cung_cap_id, the_tich_don_vi_m3, ton_toi_thieu_mac_dinh,
    #          price, trang_thai
    w("    -- 4. SẢN PHẨM (100 mặt hàng IT)")
    for p in PRODUCTS:
        mc,tn,sku,dv,dm,ncc,tv,tm,gia = p
        w(f"    INSERT INTO san_pham (ma_san_pham,ten_san_pham,sku,don_vi_tinh,danh_muc_id,doi_tac_cung_cap_id,the_tich_don_vi_m3,ton_toi_thieu_mac_dinh,price,trang_thai,ngay_tao,ngay_cap_nhat)")
        w(f"    VALUES ('{mc}','{tn}','{sku}','{dv}',")
        w(f"            (SELECT id FROM danh_muc WHERE ma_danh_muc='{dm}'),")
        w(f"            (SELECT id FROM doi_tac WHERE ma_doi_tac='{ncc}'),")
        w(f"            {tv:.4f},{tm},{gia},'HOAT_DONG'::trang_thai_san_pham,now(),now())")
        w(f"    ON CONFLICT (ma_san_pham) DO UPDATE SET ten_san_pham=EXCLUDED.ten_san_pham,the_tich_don_vi_m3=EXCLUDED.the_tich_don_vi_m3,ton_toi_thieu_mac_dinh=EXCLUDED.ton_toi_thieu_mac_dinh,price=EXCLUDED.price,ngay_cap_nhat=now();")
    w("")

    # ─────────────────────────────────────────────────────────────────
    # GIAI ĐOẠN 2: Phiếu Nhập/Xuất 2026
    # ─────────────────────────────────────────────────────────────────
    op_rng = random.Random(20260822)

    # Tích lũy tồn kho (khởi đầu 0)
    stock: Dict = {(p[0], wh): 0 for p in PRODUCTS for wh in WH_LIST}

    # ── PHIẾU NHẬP (35 phiếu) ────────────────────────────────────────
    # Sinh 35 phiếu nhập trải 8 tháng đầu 2026, 15-50 sp/lần
    INBOUND_MONTHS = [1,1,2,2,3,3,3,4,4,5,5,6,6,6,7,7,8,8,
                      1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,8]
    inbounds = []
    for i, mo in enumerate(INBOUND_MONTHS, start=1):
        day = op_rng.randint(3, 18)
        dt  = datetime.date(2026, mo, min(day, 28))
        dc  = dt.strftime("%Y%m%d")
        code = f"PNK-{dc}-{h12(f'PNK_{i}_{dc}')}"
        wh   = WH_LIST[(i-1) % 3]
        sup  = SUP_LIST[(i-1) % len(SUP_LIST)]
        mult = LOC_MULT[wh]
        # Chọn 3-5 sản phẩm
        num  = op_rng.randint(3, 5)
        sel  = PRODUCTS[((i-1)*4) % 97 : ((i-1)*4) % 97 + num]
        items, tot = [], 0.0
        for p in sel:
            qty    = max(15, min(50, int(round((20 + op_rng.randint(0,18)) * mult))))
            u_p    = round(p[8] * 0.82 / 10000) * 10000
            lt     = qty * u_p
            tot   += lt
            items.append((p[0], qty, u_p, lt))
        # Trạng thái: 30 hoàn thành, 5 dở dang
        if i <= 30:  st = "HOAN_THANH"
        elif i == 31: st = "CHO_DUYET_CAP_1"
        elif i == 32: st = "CHO_DUYET_CAP_2"
        elif i == 33: st = "CHO_KIEM_HANG"
        elif i == 34: st = "CHO_HANG_VE"
        else:         st = "NHAP"
        inbounds.append({"code":code,"date":dt,"wh":wh,"sup":sup,"status":st,"total":tot,"items":items})
        if st == "HOAN_THANH":
            for pc,qty,_,_ in items:
                stock[(pc,wh)] += qty

    inbounds.sort(key=lambda x: x["date"])

    w("    -- 5. PHIẾU NHẬP KHO (35 phiếu, 15-50 sp/lần)")
    for r in inbounds:
        code=r["code"]; d=r["date"].strftime("%Y-%m-%d")
        wh=r["wh"]; sup=r["sup"]; st=r["status"]; tot=r["total"]
        w(f"    -- {code} ({st})")
        if st == "HOAN_THANH":
            w(f"    INSERT INTO phieu_nhap_kho (ma_phieu_nhap,doi_tac_id,kho_id,trang_thai,tong_tien,ghi_chu,nguoi_tao_id,ngay_tao,nguoi_gui_duyet_id,ngay_gui_duyet,nguoi_duyet_cap_1_id,ngay_duyet_cap_1,nguoi_hoan_thanh_id,ngay_hoan_thanh,ngay_hang_ve,nguong_duyet_ap_dung,so_cap_duyet_yeu_cau,ngay_cap_nhat,version)")
            w(f"    VALUES ('{code}',(SELECT id FROM doi_tac WHERE ma_doi_tac='{sup}'),(SELECT id FROM kho WHERE ma_kho='{wh}'),'HOAN_THANH'::trang_thai_chung_tu_kho,{tot:.2f},'Nhập hàng định kỳ từ {sup}',v_emp_id,'{d} 08:00:00',v_emp_id,'{d} 09:00:00',v_manager_id,'{d} 10:00:00',v_emp_id,'{d} 14:00:00','{d} 11:30:00',50000000.00,1,'{d} 14:00:00',0) RETURNING id INTO v_pn_id;")
        elif st == "CHO_DUYET_CAP_2":
            w(f"    INSERT INTO phieu_nhap_kho (ma_phieu_nhap,doi_tac_id,kho_id,trang_thai,tong_tien,ghi_chu,nguoi_tao_id,ngay_tao,nguoi_gui_duyet_id,ngay_gui_duyet,nguoi_duyet_cap_1_id,ngay_duyet_cap_1,nguong_duyet_ap_dung,so_cap_duyet_yeu_cau,ngay_cap_nhat,version)")
            w(f"    VALUES ('{code}',(SELECT id FROM doi_tac WHERE ma_doi_tac='{sup}'),(SELECT id FROM kho WHERE ma_kho='{wh}'),'{st}'::trang_thai_chung_tu_kho,{tot:.2f},'Phiếu giá trị cao chờ duyệt cấp 2',v_emp_id,'{d} 08:30:00',v_emp_id,'{d} 09:15:00',v_manager_id,'{d} 11:00:00',50000000.00,2,'{d} 11:00:00',0) RETURNING id INTO v_pn_id;")
        elif st in ("CHO_DUYET_CAP_1","CHO_KIEM_HANG","CHO_HANG_VE"):
            w(f"    INSERT INTO phieu_nhap_kho (ma_phieu_nhap,doi_tac_id,kho_id,trang_thai,tong_tien,ghi_chu,nguoi_tao_id,ngay_tao,nguoi_gui_duyet_id,ngay_gui_duyet,nguong_duyet_ap_dung,so_cap_duyet_yeu_cau,ngay_cap_nhat,version)")
            w(f"    VALUES ('{code}',(SELECT id FROM doi_tac WHERE ma_doi_tac='{sup}'),(SELECT id FROM kho WHERE ma_kho='{wh}'),'{st}'::trang_thai_chung_tu_kho,{tot:.2f},'Phiếu luồng {st}',v_emp_id,'{d} 08:00:00',v_emp_id,'{d} 09:00:00',50000000.00,1,'{d} 09:00:00',0) RETURNING id INTO v_pn_id;")
        else:
            w(f"    INSERT INTO phieu_nhap_kho (ma_phieu_nhap,doi_tac_id,kho_id,trang_thai,tong_tien,ghi_chu,nguoi_tao_id,ngay_tao,ngay_cap_nhat,version)")
            w(f"    VALUES ('{code}',(SELECT id FROM doi_tac WHERE ma_doi_tac='{sup}'),(SELECT id FROM kho WHERE ma_kho='{wh}'),'NHAP'::trang_thai_chung_tu_kho,{tot:.2f},'Phiếu nháp đang soạn',v_emp_id,'{d} 15:00:00','{d} 15:00:00',0) RETURNING id INTO v_pn_id;")
        for pc,qty,up,lt in r["items"]:
            thuc = str(qty) if st=="HOAN_THANH" else "NULL"
            w(f"    INSERT INTO chi_tiet_phieu_nhap (phieu_nhap_id,san_pham_id,so_luong,so_luong_thuc_nhan,don_gia,thanh_tien,ghi_chu,ngay_tao,ngay_cap_nhat,version)")
            w(f"    VALUES (v_pn_id,(SELECT id FROM san_pham WHERE ma_san_pham='{pc}'),{qty},{thuc},{up:.2f},{lt:.2f},'Hàng nhập chuẩn','{d} 08:30:00','{d} 08:30:00',0);")
            if st=="HOAN_THANH":
                w(f"    INSERT INTO giao_dich_kho (san_pham_id,kho_id,loai_giao_dich,so_luong,so_luong_truoc,so_luong_sau,phieu_nhap_id,ghi_chu,nguoi_tao_id,ngay_tao)")
                w(f"    VALUES ((SELECT id FROM san_pham WHERE ma_san_pham='{pc}'),(SELECT id FROM kho WHERE ma_kho='{wh}'),'NHAP_KHO'::loai_giao_dich_kho,{qty},0,{qty},v_pn_id,'Nhập kho hoàn tất',v_emp_id,'{d} 14:00:00');")
        if st=="HOAN_THANH":
            for hd,gc,ts in [("TAO_PHIEU","Tạo phiếu nhập","08:00:00"),("GUI_DUYET","Gửi duyệt","09:00:00"),
                              ("DUYET_CAP_1","Manager duyệt","10:00:00"),("HOAN_THANH","Nhập kho hoàn tất","14:00:00")]:
                w(f"    INSERT INTO phieu_nhap_kho_lich_su (phieu_nhap_id,nguoi_thuc_hien_id,hanh_dong,ghi_chu,ngay_thuc_hien) VALUES (v_pn_id,v_emp_id,'{hd}','{gc}','{d} {ts}');")
        w("")

    # ── PHIẾU XUẤT + THUC_TE mapping ─────────────────────────────────
    # Phân chia health target trước:
    all_pairs = [(p[0], wh) for p in PRODUCTS for wh in WH_LIST]
    out_rng = random.Random(20260822)
    out_rng.shuffle(all_pairs)
    ZERO_TARGETS = set(map(tuple, all_pairs[:15]))   # ~5% → hết hàng
    LOW_TARGETS  = set(map(tuple, all_pairs[15:60])) # ~15% → sắp hết

    # actual_out: (sp_code, wh_code, date_str) → qty
    actual_out: Dict = {}

    OUTBOUND_MONTHS = [1,1,2,2,2,3,3,4,4,4,5,5,6,6,7,7,7,8,8,8,
                       1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,8,8,8,8,
                       1,2,3,4,8]
    outbounds = []
    for i, mo in enumerate(OUTBOUND_MONTHS, start=1):
        day = min(op_rng.randint(3,22), 21 if mo==8 else 28)
        dt  = datetime.date(2026, mo, day)
        d   = dt.strftime("%Y-%m-%d")
        code = f"PXK-{dt.strftime('%Y%m%d')}-{h12(f'PXK_{i}_{mo}')}"
        wh   = WH_LIST[(i-1) % 3]
        cust = CUST_LIST[(i-1) % len(CUST_LIST)]
        num  = op_rng.randint(2, 4)
        # Chọn SP có hàng
        avail = [p for p in PRODUCTS if stock[(p[0],wh)] > 3]
        if not avail: avail = [p for p in PRODUCTS if stock[(p[0],wh)] > 0]
        if not avail: avail = list(PRODUCTS[:num])
        sel_idx = ((i-1)*3) % len(avail)
        sel = avail[sel_idx: sel_idx+num]
        items, tot = [], 0.0
        for p in sel:
            pair = (p[0], wh); cur = stock[pair]; mn = p[7]
            is_last2m = (mo >= 7)
            if pair in ZERO_TARGETS and is_last2m and cur > 0:
                qty = cur   # xả hết
            elif pair in LOW_TARGETS and is_last2m and cur > mn:
                qty = max(1, cur - max(1, mn//2))  # hạ về vùng sắp hết
            else:
                max_sell = max(1, cur - mn)
                qty = min(op_rng.randint(5, 30), max_sell) if cur > mn+5 else max(1, cur//3)
            qty = max(0, min(qty, cur))
            if qty > 0:
                lt   = qty * p[8]; tot += lt
                items.append((p[0], qty, p[8], lt))
        # 40 hoàn thành, 5 dở
        if i <= 40:  st = "HOAN_THANH"
        elif i == 41: st = "CHO_DUYET_CAP_1"
        elif i == 42: st = "CHO_DUYET_CAP_1"
        elif i == 43: st = "CHO_DUYET_CAP_2"
        elif i == 44: st = "CHO_DUYET_CAP_2"
        else:         st = "NHAP"
        if st == "HOAN_THANH":
            for pc,qty,_,_ in items:
                stock[(pc,wh)] = max(0, stock[(pc,wh)] - qty)
                actual_out[(pc, wh, d)] = actual_out.get((pc,wh,d), 0) + qty
        outbounds.append({"code":code,"date":dt,"wh":wh,"cust":cust,"status":st,"total":tot,"items":items})

    outbounds.sort(key=lambda x: x["date"])

    w("    -- 6. PHIẾU XUẤT KHO (45 phiếu, 5-30 sp/lần)")
    for r in outbounds:
        code=r["code"]; d=r["date"].strftime("%Y-%m-%d")
        wh=r["wh"]; cust=r["cust"]; st=r["status"]; tot=r["total"]
        w(f"    -- {code} ({st})")
        if st == "HOAN_THANH":
            w(f"    INSERT INTO phieu_xuat_kho (ma_phieu_xuat,doi_tac_id,kho_id,trang_thai,tong_tien,ghi_chu,nguoi_tao_id,ngay_tao,nguoi_gui_duyet_id,ngay_gui_duyet,nguoi_duyet_cap_1_id,ngay_duyet_cap_1,nguoi_hoan_thanh_id,ngay_hoan_thanh,ngay_cap_nhat,version)")
            w(f"    VALUES ('{code}',(SELECT id FROM doi_tac WHERE ma_doi_tac='{cust}'),(SELECT id FROM kho WHERE ma_kho='{wh}'),'HOAN_THANH'::trang_thai_chung_tu_kho,{tot:.2f},'Xuất hàng cho {cust}',v_emp_id,'{d} 09:00:00',v_emp_id,'{d} 10:00:00',v_manager_id,'{d} 11:30:00',v_emp_id,'{d} 16:00:00','{d} 16:00:00',0) RETURNING id INTO v_px_id;")
        elif st in ("CHO_DUYET_CAP_1","CHO_DUYET_CAP_2"):
            w(f"    INSERT INTO phieu_xuat_kho (ma_phieu_xuat,doi_tac_id,kho_id,trang_thai,tong_tien,ghi_chu,nguoi_tao_id,ngay_tao,nguoi_gui_duyet_id,ngay_gui_duyet,ngay_cap_nhat,version)")
            w(f"    VALUES ('{code}',(SELECT id FROM doi_tac WHERE ma_doi_tac='{cust}'),(SELECT id FROM kho WHERE ma_kho='{wh}'),'{st}'::trang_thai_chung_tu_kho,{tot:.2f},'Phiếu xuất chờ duyệt',v_emp_id,'{d} 09:00:00',v_emp_id,'{d} 10:00:00','{d} 10:00:00',0) RETURNING id INTO v_px_id;")
        else:
            w(f"    INSERT INTO phieu_xuat_kho (ma_phieu_xuat,doi_tac_id,kho_id,trang_thai,tong_tien,ghi_chu,nguoi_tao_id,ngay_tao,ngay_cap_nhat,version)")
            w(f"    VALUES ('{code}',(SELECT id FROM doi_tac WHERE ma_doi_tac='{cust}'),(SELECT id FROM kho WHERE ma_kho='{wh}'),'NHAP'::trang_thai_chung_tu_kho,{tot:.2f},'Phiếu xuất nháp',v_emp_id,'{d} 14:00:00','{d} 14:00:00',0) RETURNING id INTO v_px_id;")
        for pc,qty,up,lt in r["items"]:
            w(f"    INSERT INTO chi_tiet_phieu_xuat (phieu_xuat_id,san_pham_id,so_luong,don_gia,thanh_tien,ghi_chu,ngay_tao,ngay_cap_nhat,version)")
            w(f"    VALUES (v_px_id,(SELECT id FROM san_pham WHERE ma_san_pham='{pc}'),{qty},{up:.2f},{lt:.2f},'Xuất kho tiêu chuẩn','{d} 09:30:00','{d} 09:30:00',0);")
            if st=="HOAN_THANH":
                w(f"    INSERT INTO giao_dich_kho (san_pham_id,kho_id,loai_giao_dich,so_luong,so_luong_truoc,so_luong_sau,phieu_xuat_id,ghi_chu,nguoi_tao_id,ngay_tao)")
                w(f"    VALUES ((SELECT id FROM san_pham WHERE ma_san_pham='{pc}'),(SELECT id FROM kho WHERE ma_kho='{wh}'),'XUAT_KHO'::loai_giao_dich_kho,{qty},{qty+stock.get((pc,wh),0)},0,v_px_id,'Xuất kho hoàn tất',v_emp_id,'{d} 16:00:00');")
        if st=="HOAN_THANH":
            for hd,gc,ts in [("TAO_PHIEU","Tạo phiếu xuất","09:00:00"),("GUI_DUYET","Gửi duyệt","10:00:00"),
                              ("DUYET","Manager duyệt","11:30:00"),("HOAN_THANH","Xuất kho hoàn tất","16:00:00")]:
                w(f"    INSERT INTO phieu_xuat_kho_lich_su (phieu_xuat_id,nguoi_thuc_hien_id,hanh_dong,ghi_chu,ngay_thuc_hien) VALUES (v_px_id,v_emp_id,'{hd}','{gc}','{d} {ts}');")
        w("")

    # ── TỒN KHO ──────────────────────────────────────────────────────
    w("    -- 7. TỒN KHO")
    w("    INSERT INTO ton_kho (san_pham_id,kho_id,so_luong,ngay_cap_nhat)")
    w("    SELECT sp.id,k.id,")
    w("           COALESCE(SUM(CASE WHEN gdk.loai_giao_dich IN ('NHAP_KHO','NHAP_DAU_KY','DIEU_CHINH_TANG') THEN gdk.so_luong")
    w("                            WHEN gdk.loai_giao_dich IN ('XUAT_KHO','DIEU_CHINH_GIAM') THEN -gdk.so_luong ELSE 0 END),0),now()")
    w("    FROM san_pham sp CROSS JOIN kho k")
    w("    LEFT JOIN giao_dich_kho gdk ON gdk.san_pham_id=sp.id AND gdk.kho_id=k.id")
    w("    GROUP BY sp.id,k.id;")
    w("")

    # ── CẤU HÌNH TỒN KHO ─────────────────────────────────────────────
    # Column: ton_toi_thieu_ghi_de (WarehouseStockConfig entity)
    w("    -- 8. CẤU HÌNH TỒN KHO (cau_hinh_ton_kho)")
    for p in PRODUCTS:
        mc,_,_,_,_,_,_,mn,_ = p
        for wh,f in [("K001",1.0),("K002",0.8),("K003",1.2)]:
            ov = max(2, int(mn*f))
            w(f"    INSERT INTO cau_hinh_ton_kho (san_pham_id,kho_id,ton_toi_thieu_ghi_de,ngay_tao,ngay_cap_nhat)")
            w(f"    VALUES ((SELECT id FROM san_pham WHERE ma_san_pham='{mc}'),(SELECT id FROM kho WHERE ma_kho='{wh}'),{ov},now(),now());")
    w("")

    # ── KIỂM KÊ ──────────────────────────────────────────────────────
    # dot_kiem_ke.ma_dot (NOT ma_dot_kiem_ke)
    # dot_kiem_ke has NO ten_dot_kiem_ke column
    w("    -- 9. ĐỢT KIỂM KÊ (12 đợt)")
    for i in range(1, 13):
        mo = ((i-1) % 8)+1
        dt_s = f"2026-{mo:02d}-28"
        dkk  = f"DKK-2026{mo:02d}28-{h12(f'DKK_{i}_{mo}')}"
        wh   = WH_LIST[(i-1) % 3]
        gc_dkk = f'Kiem ke thang {mo} tai {wh}'  # ASCII only to avoid encoding issue
        # InventoryCountStatus: plain @Enumerated STRING, not NAMED_ENUM -> no cast needed
        w(f"    INSERT INTO dot_kiem_ke (ma_dot,kho_id,trang_thai,ghi_chu,nguoi_tao_id,ngay_tao,nguoi_chot_id,ngay_chot,ngay_cap_nhat,version)")
        w(f"    VALUES ('{dkk}',(SELECT id FROM kho WHERE ma_kho='{wh}'),'DA_CHOT','{gc_dkk}',v_emp_id,'{dt_s} 16:30:00',v_manager_id,'{dt_s} 18:00:00','{dt_s} 18:00:00',0) RETURNING id INTO v_dkk_id;")
        for idx in range(1, 4):
            si = ((i*3+idx) % 95)+1
            sc = f"SP{si:03d}"
            # chi_tiet_kiem_ke: so_luong_he_thong, so_luong_thuc_te, chenh_lech
            w(f"    INSERT INTO chi_tiet_kiem_ke (dot_kiem_ke_id,san_pham_id,so_luong_he_thong,so_luong_thuc_te,chenh_lech,ghi_chu,version)")
            w(f"    VALUES (v_dkk_id,(SELECT id FROM san_pham WHERE ma_san_pham='{sc}'),20,20,0,'Khớp sổ sách 100%',0);")
        w("")

    # ── BIÊN BẢN CHÊNH LỆCH ──────────────────────────────────────────
    # bien_ban_chenh_lech: nguoi_lap_id, trang_thai ('CHO_DUYET'/'DA_DUYET')
    # chi_tiet_bien_ban_chenh_lech: so_luong_lech, ly_do, huong_xu_ly
    w("    -- 10. BIÊN BẢN CHÊNH LỆCH")
    for wh,sc,pi in [("K001","SP015",1),("K002","SP020",2),("K003","SP026",3)]:
        pn_target = inbounds[pi]["code"]
        bb = f"BBCL-20260428-{h12(f'BBCL_{wh}')}"
        w(f"    INSERT INTO bien_ban_chenh_lech (phieu_nhap_id,ma_bien_ban,trang_thai,nguoi_lap_id,ngay_lap,nguoi_duyet_id,ngay_duyet,ghi_chu,ngay_tao,ngay_cap_nhat,version)")
        w(f"    VALUES ((SELECT id FROM phieu_nhap_kho WHERE ma_phieu_nhap='{pn_target}'),'{bb}','DA_DUYET',v_emp_id,'2026-04-28 14:30:00',v_manager_id,'2026-04-28 15:30:00','Xử lý thừa thiếu nhập',now(),now(),0);")
        w(f"    INSERT INTO chi_tiet_bien_ban_chenh_lech (bien_ban_id,san_pham_id,so_luong_chung_tu,so_luong_thuc_te,so_luong_lech,ly_do,huong_xu_ly,ngay_tao,ngay_cap_nhat,version)")
        w(f"    VALUES ((SELECT id FROM bien_ban_chenh_lech WHERE ma_bien_ban='{bb}'),(SELECT id FROM san_pham WHERE ma_san_pham='{sc}'),25,24,-1,'Vỡ khi vận chuyển','NCC bù hàng đợt sau',now(),now(),0);")
    w("")

    # ── CẢNH BÁO TỒN KHO ─────────────────────────────────────────────
    # canh_bao_ton_kho: so_luong_hien_tai, ton_toi_thieu, muc_do, trang_thai, ghi_chu
    # muc_do values: 'LOW','MEDIUM','HIGH' (InventoryAlertSeverity enum)
    # trang_thai values: 'OPEN','ACKNOWLEDGED','RESOLVED' (InventoryAlertStatus enum)
    w("    -- 11. CẢNH BÁO TỒN KHO")
    alert_cnt = 0
    for p in PRODUCTS:
        mc=p[0]; mn=p[7]
        for wh in WH_LIST:
            cur = stock.get((mc,wh), 0)
            if cur == 0:
                w(f"    INSERT INTO canh_bao_ton_kho (san_pham_id,kho_id,so_luong_hien_tai,ton_toi_thieu,muc_do,trang_thai,ghi_chu,ngay_tao,ngay_cap_nhat,version)")
                w(f"    VALUES ((SELECT id FROM san_pham WHERE ma_san_pham='{mc}'),(SELECT id FROM kho WHERE ma_kho='{wh}'),0,{mn},'HIGH','OPEN','Sản phẩm {mc} tại {wh} đã hết hàng - cần đặt hàng ngay!',now(),now(),0);")
                alert_cnt += 1
            elif cur <= mn:
                sev = "MEDIUM" if cur <= mn//2 else "LOW"
                w(f"    INSERT INTO canh_bao_ton_kho (san_pham_id,kho_id,so_luong_hien_tai,ton_toi_thieu,muc_do,trang_thai,ghi_chu,ngay_tao,ngay_cap_nhat,version)")
                w(f"    VALUES ((SELECT id FROM san_pham WHERE ma_san_pham='{mc}'),(SELECT id FROM kho WHERE ma_kho='{wh}'),{cur},{mn},'{sev}','OPEN','Sản phẩm {mc} tại {wh} sắp hết ({cur}/{mn})',now(),now(),0);")
                alert_cnt += 1
    w("")

    # ── CẢNH BÁO SỨC CHỨA ────────────────────────────────────────────
    # canh_bao_suc_chua_kho: used_capacity_m3, max_capacity_m3, usage_percentage, severity, status, message
    w("    -- 12. CẢNH BÁO SỨC CHỨA KHO")
    w("    INSERT INTO canh_bao_suc_chua_kho (kho_id,used_capacity_m3,max_capacity_m3,usage_percentage,severity,status,message,ngay_tao,ngay_cap_nhat)")
    w("    VALUES ((SELECT id FROM kho WHERE ma_kho='K003'),2780.500,3000.000,92.68,'HIGH','OPEN','Kho Hồ Chí Minh đạt 92.68% sức chứa!',now(),now());")
    w("    INSERT INTO canh_bao_suc_chua_kho (kho_id,used_capacity_m3,max_capacity_m3,usage_percentage,severity,status,message,ngay_tao,ngay_cap_nhat)")
    w("    VALUES ((SELECT id FROM kho WHERE ma_kho='K001'),2150.000,2500.000,86.00,'MEDIUM','OPEN','Kho Hà Nội đạt 86.00% sức chứa!',now(),now());")
    w("")

    # ─────────────────────────────────────────────────────────────────
    # GIAI ĐOẠN 1: SEED AI 2025 (180 ngày: 05/07 → 31/12/2025)
    # + GIAI ĐOẠN 2: Time-series 2026 với padding đầy đủ
    # ─────────────────────────────────────────────────────────────────
    w("    -- ================================================================")
    w("    -- 13. AI TIME-SERIES")
    w("    -- GĐ1 (SEED): 05/07/2025 → 31/12/2025 (180 ngày × 100 SP × 3 KHO)")
    w("    -- GĐ2 (THUC_TE): 01/01/2026 → 21/08/2026 với full day-padding")
    w("    -- ================================================================")

    ai_rng = random.Random(42)

    # Sinh toàn bộ time-series
    d_seed_start = datetime.date(2025, 7, 5)
    d_seed_end   = datetime.date(2025, 12, 31)
    d_real_start = datetime.date(2026, 1, 1)
    d_real_end   = datetime.date(2026, 8, 21)

    all_rows = []

    for p in PRODUCTS:
        pc = p[0]; base = max(2, int(p[7] * 0.45))

        for wh in WH_LIST:
            loc_m = LOC_MULT[wh]

            # ── GĐ1: SEED 2025 ──
            cur = d_seed_start
            while cur <= d_seed_end:
                ds = cur.strftime("%Y-%m-%d")
                is_wk = (cur.weekday() >= 5)
                mo = cur.month
                day = cur.day
                wlift   = 1.5 if is_wk else 1.0
                if mo == 11:          slift = 2.2
                elif mo==12 and day>=20: slift = 1.8
                elif mo in (7,8):     slift = 1.2
                else:                 slift = 1.0
                noise = ai_rng.uniform(-1.5, 2.5)
                zero_prob = 0.18
                if ai_rng.random() < zero_prob:
                    qty = 0
                else:
                    qty = max(0, int(round(base * loc_m * wlift * slift + noise)))
                all_rows.append((pc, wh, ds, qty, "SEED"))
                cur += datetime.timedelta(days=1)

            # ── GĐ2: THUC_TE 2026 — padding đầy đủ ──
            cur = d_real_start
            while cur <= d_real_end:
                ds = cur.strftime("%Y-%m-%d")
                key = (pc, wh, ds)
                if key in actual_out:
                    qty = actual_out[key]
                    src = "THUC_TE"
                else:
                    # Ngày không có xuất → padding qty=0, vẫn nguon='THUC_TE'
                    qty = 0
                    src = "THUC_TE"
                all_rows.append((pc, wh, ds, qty, src))
                cur += datetime.timedelta(days=1)

    w("END $$;")
    w("")

    print(f"[INFO] Tong ban ghi AI time-series: {len(all_rows):,}")

    # Ghi batch 1000 dòng/lần
    BATCH = 1000
    for b in range(0, len(all_rows), BATCH):
        chunk = all_rows[b: b+BATCH]
        w("    INSERT INTO ai.lich_su_ban_hang (san_pham_id,kho_id,ngay,so_luong,nguon,ngay_tao)")
        w("    VALUES")
        vals = []
        for pc, wh, ds, qty, src in chunk:
            vals.append(
                f"        ((SELECT id FROM san_pham WHERE ma_san_pham='{pc}'),"
                f"(SELECT id FROM kho WHERE ma_kho='{wh}'),'{ds}',{qty},'{src}',now())"
            )
        w(",\n".join(vals))
        w("    ON CONFLICT (san_pham_id,kho_id,ngay) DO UPDATE")
        w("        SET so_luong=EXCLUDED.so_luong,nguon=EXCLUDED.nguon,ngay_tao=now();")
        w("")

    w("")

    # ── RESET SEQUENCES ───────────────────────────────────────────────
    w("-- ================================================================")
    w("-- RESET SEQUENCES (20 sequences)")
    w("-- ================================================================")
    SEQS = [
        ("danh_muc_id_seq",                    "danh_muc"),
        ("kho_id_seq",                         "kho"),
        ("doi_tac_id_seq",                     "doi_tac"),
        ("san_pham_id_seq",                    "san_pham"),
        ("ton_kho_id_seq",                     "ton_kho"),
        ("cau_hinh_ton_kho_id_seq",            "cau_hinh_ton_kho"),
        ("phieu_nhap_kho_id_seq",              "phieu_nhap_kho"),
        ("chi_tiet_phieu_nhap_id_seq",         "chi_tiet_phieu_nhap"),
        ("phieu_nhap_kho_lich_su_id_seq",      "phieu_nhap_kho_lich_su"),
        ("phieu_xuat_kho_id_seq",              "phieu_xuat_kho"),
        ("chi_tiet_phieu_xuat_id_seq",         "chi_tiet_phieu_xuat"),
        ("phieu_xuat_kho_lich_su_id_seq",      "phieu_xuat_kho_lich_su"),
        ("giao_dich_kho_id_seq",               "giao_dich_kho"),
        ("dot_kiem_ke_id_seq",                 "dot_kiem_ke"),
        ("chi_tiet_kiem_ke_id_seq",            "chi_tiet_kiem_ke"),
        ("bien_ban_chenh_lech_id_seq",         "bien_ban_chenh_lech"),
        ("chi_tiet_bien_ban_chenh_lech_id_seq","chi_tiet_bien_ban_chenh_lech"),
        ("canh_bao_ton_kho_id_seq",            "canh_bao_ton_kho"),
        ("canh_bao_suc_chua_kho_id_seq",       "canh_bao_suc_chua_kho"),
        ("ai.lich_su_ban_hang_id_seq",         "ai.lich_su_ban_hang"),
    ]
    for seq, tbl in SEQS:
        w(f"SELECT setval('{seq}', COALESCE((SELECT MAX(id) FROM {tbl}),1), true);")

    return "\n".join(L)


if __name__ == "__main__":
    print("[INFO] Sinh V45 SQL (Prompt Master v5.0)...")
    sql = build_sql()
    with open(OUTPUT_SQL_PATH, "w", encoding="utf-8") as f:
        f.write(sql)
    print(f"[SUCCESS] → {OUTPUT_SQL_PATH}")
    print(f"          Dòng: {len(sql.splitlines()):,}  |  Kích thước: {len(sql):,} bytes")
