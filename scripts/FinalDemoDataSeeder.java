import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.BufferedReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinalDemoDataSeeder {
    private static final Path OUT = Path.of("target", "final-demo-data");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 22, 15, 0);

    record Inv(String product, String warehouse, int qty, int min, String scenario, BigDecimal avg) {}
    record Model(String scenario, String product, String warehouse, int version, BigDecimal smape, BigDecimal mae,
                 BigDecimal rmse, int days, String mode, String dataset, LocalDate start, LocalDate end,
                 LocalDateTime trainedAt) {}
    record Forecast(String scenario, int horizon, LocalDate date, BigDecimal qty) {}
    record Daily(String scenario, LocalDate date, BigDecimal qty) {}

    public static void main(String[] args) throws Exception {
        requireEnv("DB_URL");
        requireEnv("DB_USERNAME");
        requireEnv("DB_PASSWORD");
        List<Inv> inventory = readInventory();
        List<Model> models = readModels();
        List<Forecast> forecasts = readForecasts();
        List<Daily> daily = readDaily();

        try (Connection c = DriverManager.getConnection(
                System.getenv("DB_URL"), System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"))) {
            c.setAutoCommit(false);
            try {
                Map<String, Long> products = ids(c, "san_pham", "ma_san_pham");
                Map<String, Long> warehouses = ids(c, "kho", "ma_kho");
                long manager = employee(c, "ADMIN", "MANAGER");
                long employee = employee(c, "EMPLOYEE");
                Long supplier = scalarLong(c, "SELECT id FROM doi_tac WHERE loai_doi_tac='NHA_CUNG_CAP' ORDER BY id LIMIT 1");
                Long customer = scalarLong(c, "SELECT id FROM doi_tac WHERE loai_doi_tac='KHACH_HANG' ORDER BY id LIMIT 1");
                checkIds(inventory, products, warehouses);

                cleanup(c);
                copyHistory(c, products, warehouses);
                seedInventory(c, inventory, products, warehouses, manager);
                Map<String, Long> modelIds = seedForecast(c, models, forecasts, daily, products, warehouses);
                seedDocuments(c, inventory, products, warehouses, manager, supplier, customer);
                seedCounts(c, inventory, products, warehouses, manager);
                seedAlerts(c, inventory, products, warehouses);
                seedAiRequests(c, models, modelIds, inventory, products, warehouses, manager, employee);
                validate(c);
                c.commit();
                System.out.println("final_demo_data_seeded");
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static void cleanup(Connection c) throws Exception {
        exec(c, "DELETE FROM yeu_cau_nhap_hang_ai");
        exec(c, "DELETE FROM ai.ket_qua_du_bao_hang_ngay WHERE thong_tin_mo_hinh_id IN (SELECT id FROM ai.thong_tin_mo_hinh WHERE kieu_tap_du_lieu='EXTERNAL')");
        exec(c, "DELETE FROM ai.ket_qua_du_bao WHERE thong_tin_mo_hinh_id IN (SELECT id FROM ai.thong_tin_mo_hinh WHERE kieu_tap_du_lieu='EXTERNAL')");
        exec(c, "DELETE FROM ai.nhat_ky_lech_mo_hinh WHERE thong_tin_mo_hinh_id IN (SELECT id FROM ai.thong_tin_mo_hinh WHERE kieu_tap_du_lieu='EXTERNAL')");
        exec(c, "DELETE FROM ai.thong_tin_mo_hinh WHERE kieu_tap_du_lieu='EXTERNAL'");
        exec(c, "DELETE FROM ai.lich_su_ban_hang WHERE nguon_du_lieu='EXTERNAL_RETAIL'");
        exec(c, "DELETE FROM phieu_nhap_kho_lich_su");
        exec(c, "DELETE FROM phieu_xuat_kho_lich_su");
        execIfExists(c, "chi_tiet_bien_ban_chenh_lech", "DELETE FROM chi_tiet_bien_ban_chenh_lech");
        execIfExists(c, "bien_ban_chenh_lech", "DELETE FROM bien_ban_chenh_lech");
        exec(c, "DELETE FROM giao_dich_kho");
        exec(c, "DELETE FROM chi_tiet_phieu_nhap");
        exec(c, "DELETE FROM chi_tiet_phieu_xuat");
        exec(c, "DELETE FROM phieu_nhap_kho");
        exec(c, "DELETE FROM phieu_xuat_kho");
        exec(c, "DELETE FROM chi_tiet_kiem_ke");
        exec(c, "DELETE FROM dot_kiem_ke");
        exec(c, "DELETE FROM canh_bao_ton_kho");
        exec(c, "DELETE FROM cau_hinh_ton_kho");
        exec(c, "DELETE FROM ton_kho");
    }

    private static void copyHistory(Connection c, Map<String, Long> products, Map<String, Long> warehouses) throws Exception {
        Path src = OUT.resolve("external-retail-history.csv");
        StringBuilder tsv = new StringBuilder();
        try (BufferedReader r = Files.newBufferedReader(src)) {
            r.readLine();
            for (String line; (line = r.readLine()) != null; ) {
                String[] v = line.split(",", -1);
                tsv.append(products.get(v[0])).append('\t')
                        .append(warehouses.get(v[1])).append('\t')
                        .append(v[2]).append('\t')
                        .append(v[3]).append('\t')
                        .append(v[4].isBlank() ? "\\N" : v[4]).append('\t')
                        .append("EXTERNAL_RETAIL").append('\t')
                        .append(v[5]).append('\t')
                        .append(NOW).append('\t')
                        .append(NOW).append('\n');
            }
        }
        CopyManager copy = new CopyManager(c.unwrap(BaseConnection.class));
        try (Reader reader = new java.io.StringReader(tsv.toString())) {
            copy.copyIn("""
                    COPY ai.lich_su_ban_hang
                    (san_pham_id,kho_id,ngay,so_luong,gia_ban_binh_quan,nguon_du_lieu,tham_chieu_nguon,ngay_tao,ngay_cap_nhat)
                    FROM STDIN WITH (FORMAT text)
                    """, reader);
        }
    }

    private static void seedInventory(Connection c, List<Inv> rows, Map<String, Long> products,
                                      Map<String, Long> warehouses, long actor) throws Exception {
        try (PreparedStatement stock = c.prepareStatement("""
                INSERT INTO ton_kho(san_pham_id,kho_id,so_luong,ngay_cap_nhat) VALUES(?,?,?,?)
                """);
             PreparedStatement cfg = c.prepareStatement("""
                INSERT INTO cau_hinh_ton_kho(san_pham_id,kho_id,ton_toi_thieu_ghi_de,ngay_tao,ngay_cap_nhat)
                VALUES(?,?,?,?,?)
                """);
             PreparedStatement tx = c.prepareStatement("""
                INSERT INTO giao_dich_kho(san_pham_id,kho_id,loai_giao_dich,so_luong,so_luong_truoc,so_luong_sau,ghi_chu,nguoi_tao_id,ngay_tao)
                VALUES(?,?,?::loai_giao_dich_kho,?,?,?,?,?,?)
                """)) {
            for (Inv r : rows) {
                long p = products.get(r.product), w = warehouses.get(r.warehouse);
                stock.setLong(1, p); stock.setLong(2, w); stock.setInt(3, r.qty); stock.setTimestamp(4, ts(NOW)); stock.addBatch();
                cfg.setLong(1, p); cfg.setLong(2, w); cfg.setInt(3, r.min); cfg.setTimestamp(4, ts(NOW)); cfg.setTimestamp(5, ts(NOW)); cfg.addBatch();
                tx.setLong(1, p); tx.setLong(2, w); tx.setString(3, "NHAP_DAU_KY"); tx.setInt(4, r.qty);
                tx.setInt(5, 0); tx.setInt(6, r.qty); tx.setString(7, "Final demo opening balance"); tx.setLong(8, actor); tx.setTimestamp(9, ts(NOW)); tx.addBatch();
            }
            stock.executeBatch(); cfg.executeBatch(); tx.executeBatch();
        }
    }

    private static Map<String, Long> seedForecast(Connection c, List<Model> models, List<Forecast> forecasts,
                                                   List<Daily> daily, Map<String, Long> products,
                                                   Map<String, Long> warehouses) throws Exception {
        Map<String, Long> modelIds = new HashMap<>();
        try (PreparedStatement m = c.prepareStatement("""
                INSERT INTO ai.thong_tin_mo_hinh(san_pham_id,kho_id,smape,phien_ban,so_ngay_du_lieu,che_do,kieu_tap_du_lieu,
                ngay_bat_dau_du_lieu,ngay_ket_thuc_du_lieu,mae,rmse,tham_so_mo_hinh,dac_trung_su_dung,ngay_huan_luyen)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,'{"source":"final-demo"}'::jsonb,'["lag","rolling","calendar"]'::jsonb,?)
                RETURNING id
                """)) {
            for (Model r : models) {
                m.setLong(1, products.get(r.product)); m.setLong(2, warehouses.get(r.warehouse)); m.setBigDecimal(3, r.smape);
                m.setInt(4, r.version); m.setInt(5, r.days); m.setString(6, r.mode); m.setString(7, r.dataset);
                m.setDate(8, Date.valueOf(r.start)); m.setDate(9, Date.valueOf(r.end)); m.setBigDecimal(10, r.mae);
                m.setBigDecimal(11, r.rmse); m.setTimestamp(12, ts(r.trainedAt));
                try (ResultSet rs = m.executeQuery()) { rs.next(); modelIds.put(r.scenario, rs.getLong(1)); }
            }
        }
        try (PreparedStatement f = c.prepareStatement("""
                INSERT INTO ai.ket_qua_du_bao(thong_tin_mo_hinh_id,san_pham_id,kho_id,ngay_du_bao,so_ngay_du_bao,
                so_luong_du_bao,phien_ban,horizon_days,ngay_moc_du_bao,nhu_cau_trung_binh_ngay,ngay_tao)
                VALUES(?,?,?,?,?,?,?,?,?,?,?)
                """);
             PreparedStatement d = c.prepareStatement("""
                INSERT INTO ai.ket_qua_du_bao_hang_ngay(thong_tin_mo_hinh_id,ngay_du_bao,so_luong_du_bao,ngay_tao)
                VALUES(?,?,?,?)
                """)) {
            Map<String, Model> byScenario = new HashMap<>();
            for (Model model : readModels()) byScenario.put(model.scenario, model);
            for (Forecast r : forecasts) {
                Model model = byScenario.get(r.scenario);
                f.setLong(1, modelIds.get(r.scenario)); f.setLong(2, products.get(model.product)); f.setLong(3, warehouses.get(model.warehouse));
                f.setDate(4, Date.valueOf(r.date)); f.setInt(5, r.horizon); f.setBigDecimal(6, r.qty); f.setInt(7, model.version);
                f.setShort(8, (short) r.horizon); f.setDate(9, Date.valueOf(LocalDate.of(2024, 9, 26))); f.setBigDecimal(10, r.qty); f.setTimestamp(11, ts(NOW)); f.addBatch();
            }
            for (Daily r : daily) {
                d.setLong(1, modelIds.get(r.scenario)); d.setDate(2, Date.valueOf(r.date)); d.setBigDecimal(3, r.qty); d.setTimestamp(4, ts(NOW)); d.addBatch();
            }
            f.executeBatch(); d.executeBatch();
        }
        return modelIds;
    }

    private static void seedDocuments(Connection c, List<Inv> inv, Map<String, Long> products, Map<String, Long> warehouses,
                                      long actor, Long supplier, Long customer) throws Exception {
        for (int i = 0; i < 6; i++) {
            Inv r = inv.get(i * 9);
            long id = receipt(c, "phieu_nhap_kho", "ma_phieu_nhap", "FD-PN-" + (i + 1), r.warehouse,
                    i == 0 ? "CHO_DUYET_CAP_1" : i == 1 ? "CHO_DUYET_CAP_2" : i == 2 ? "CHO_KIEM_HANG" : "HOAN_THANH",
                    products, warehouses, actor, supplier, r.qty * 100000L);
            receiptDetail(c, "chi_tiet_phieu_nhap", "phieu_nhap_id", id, products.get(r.product), Math.max(5, r.min / 4), 100000);
            hist(c, "phieu_nhap_kho_lich_su", "phieu_nhap_id", id, actor, "TAO", "Final demo inbound");
        }
        for (int i = 0; i < 5; i++) {
            Inv r = inv.get(60 + i * 7);
            long id = receipt(c, "phieu_xuat_kho", "ma_phieu_xuat", "FD-PX-" + (i + 1), r.warehouse,
                    i == 0 ? "CHO_DUYET_CAP_1" : i == 1 ? "CHO_DUYET_CAP_2" : "HOAN_THANH",
                    products, warehouses, actor, customer, r.qty * 120000L);
            receiptDetail(c, "chi_tiet_phieu_xuat", "phieu_xuat_id", id, products.get(r.product), Math.max(3, r.qty / 8), 120000);
            hist(c, "phieu_xuat_kho_lich_su", "phieu_xuat_id", id, actor, "TAO", "Final demo outbound");
        }
    }

    private static void seedCounts(Connection c, List<Inv> inv, Map<String, Long> products, Map<String, Long> warehouses, long actor) throws Exception {
        Inv a = inv.get(20), b = inv.get(21), open = inv.get(22);
        count(c, "FD-KK-1", a, a.qty, products, warehouses, actor, "DA_CHOT");
        count(c, "FD-KK-2", b, b.qty + 5, products, warehouses, actor, "DA_CHOT");
        exec(c, "UPDATE ton_kho SET so_luong=so_luong+5 WHERE san_pham_id=" + products.get(b.product) + " AND kho_id=" + warehouses.get(b.warehouse));
        exec(c, "INSERT INTO giao_dich_kho(san_pham_id,kho_id,loai_giao_dich,so_luong,so_luong_truoc,so_luong_sau,ghi_chu,nguoi_tao_id,ngay_tao) VALUES (" +
                products.get(b.product) + "," + warehouses.get(b.warehouse) + ",'DIEU_CHINH_TANG',5," + b.qty + "," + (b.qty + 5) + ",'Final demo count adjustment'," + actor + ",'" + NOW + "')");
        count(c, "FD-KK-3", open, null, products, warehouses, actor, "DANG_KIEM_KE");
    }

    private static void seedAlerts(Connection c, List<Inv> rows, Map<String, Long> products, Map<String, Long> warehouses) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO canh_bao_ton_kho(san_pham_id,kho_id,so_luong_hien_tai,ton_toi_thieu,ton_toi_da,muc_do,trang_thai,ghi_chu,version,ngay_tao,ngay_cap_nhat)
                VALUES(?,?,?,?,?,?,?,?,0,?,?)
                """)) {
            for (Inv r : rows) {
                if (r.qty >= r.min) continue;
                ps.setLong(1, products.get(r.product)); ps.setLong(2, warehouses.get(r.warehouse));
                ps.setInt(3, r.qty); ps.setInt(4, r.min); ps.setInt(5, r.min * 4);
                ps.setString(6, r.qty * 2 < r.min ? "CRITICAL" : "WARNING"); ps.setString(7, "OPEN");
                ps.setString(8, "Final demo low stock"); ps.setTimestamp(9, ts(NOW)); ps.setTimestamp(10, ts(NOW)); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedAiRequests(Connection c, List<Model> models, Map<String, Long> modelIds, List<Inv> inv,
                                       Map<String, Long> products, Map<String, Long> warehouses, long manager, long employee) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO yeu_cau_nhap_hang_ai(ma_yeu_cau,thong_tin_mo_hinh_id,san_pham_id,kho_id,horizon_days,
                so_luong_ai_goi_y,so_luong_yeu_cau,nguoi_gui_id,nguoi_nhan_id,noi_dung,trang_thai,trang_thai_email,ngay_tao,ngay_cap_nhat)
                VALUES(?,?,?,?,30,?,?,?,?,?,'DA_GUI','CHO_GUI',?,?)
                """)) {
            int i = 1;
            for (Model m : models) {
                int requestQty = inv.stream().filter(x -> x.product.equals(m.product) && x.warehouse.equals(m.warehouse)).findFirst().map(x -> Math.max(20, x.min - x.qty)).orElse(50);
                ps.setString(1, "FD-AI-" + i++); ps.setLong(2, modelIds.get(m.scenario)); ps.setLong(3, products.get(m.product));
                ps.setLong(4, warehouses.get(m.warehouse)); ps.setInt(5, requestQty); ps.setInt(6, requestQty);
                ps.setLong(7, manager); ps.setLong(8, employee); ps.setString(9, "Final demo AI replenishment request");
                ps.setTimestamp(10, ts(NOW)); ps.setTimestamp(11, ts(NOW)); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void validate(Connection c) throws Exception {
        assertCount(c, "SELECT count(*) FROM ai.lich_su_ban_hang WHERE nguon_du_lieu='EXTERNAL_RETAIL'", 108600);
        assertCount(c, "SELECT count(*) FROM ton_kho", 300);
        assertCount(c, "SELECT count(*) FROM cau_hinh_ton_kho", 300);
        assertCount(c, "SELECT count(*) FROM ai.thong_tin_mo_hinh WHERE kieu_tap_du_lieu='EXTERNAL'", 3);
        assertCount(c, "SELECT count(*) FROM ai.ket_qua_du_bao_hang_ngay d JOIN ai.thong_tin_mo_hinh m ON m.id=d.thong_tin_mo_hinh_id WHERE m.kieu_tap_du_lieu='EXTERNAL'", 90);
        assertCount(c, "SELECT count(*) FROM ai.ket_qua_du_bao f JOIN ai.thong_tin_mo_hinh m ON m.id=f.thong_tin_mo_hinh_id WHERE m.kieu_tap_du_lieu='EXTERNAL'", 9);
        assertCount(c, "SELECT count(*) FROM phieu_nhap_kho WHERE trang_thai IN ('CHO_DUYET_CAP_1','CHO_DUYET_CAP_2')", 2);
        assertCount(c, "SELECT count(*) FROM phieu_xuat_kho WHERE trang_thai IN ('CHO_DUYET_CAP_1','CHO_DUYET_CAP_2')", 2);
        assertCount(c, "SELECT count(*) FROM dot_kiem_ke", 3);
        long dup = scalarLong(c, """
                SELECT count(*) FROM (
                  SELECT san_pham_id,kho_id,ngay,nguon_du_lieu,count(*) c
                  FROM ai.lich_su_ban_hang WHERE nguon_du_lieu='EXTERNAL_RETAIL'
                  GROUP BY 1,2,3,4 HAVING count(*)>1
                ) d
                """);
        if (dup != 0) throw new IllegalStateException("duplicate history rows: " + dup);
        long neg = scalarLong(c, "SELECT count(*) FROM ton_kho WHERE so_luong < 0");
        if (neg != 0) throw new IllegalStateException("negative stock rows: " + neg);
    }

    private static long receipt(Connection c, String table, String codeCol, String code, String warehouse, String status,
                                Map<String, Long> products, Map<String, Long> warehouses, long actor, Long partner, long total) throws Exception {
        String partnerCol = table.equals("phieu_nhap_kho") ? "doi_tac_id" : "doi_tac_id";
        String extraCols = table.equals("phieu_nhap_kho") ? ",nguong_duyet_ap_dung,so_cap_duyet_yeu_cau" : "";
        String extraVals = table.equals("phieu_nhap_kho") ? ",?,?" : "";
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO " + table + "(" + codeCol + ",kho_id," + partnerCol +
                ",trang_thai,tong_tien,ghi_chu,nguoi_tao_id,nguoi_gui_duyet_id,ngay_gui_duyet" + extraCols + ",ngay_tao,ngay_cap_nhat,version) " +
                "VALUES(?,?,?,?::trang_thai_chung_tu_kho,?,?,?,?,?" + extraVals + ",?,?,0) RETURNING id")) {
            ps.setString(1, code); ps.setLong(2, warehouses.get(warehouse)); setLongOrNull(ps, 3, partner);
            ps.setString(4, status); ps.setBigDecimal(5, BigDecimal.valueOf(total)); ps.setString(6, "Final demo document");
            ps.setLong(7, actor); ps.setLong(8, actor); ps.setTimestamp(9, ts(NOW));
            int i = 10;
            if (table.equals("phieu_nhap_kho")) {
                ps.setBigDecimal(i++, BigDecimal.valueOf(50000000));
                ps.setShort(i++, (short) 2);
            }
            ps.setTimestamp(i++, ts(NOW)); ps.setTimestamp(i, ts(NOW));
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    private static void receiptDetail(Connection c, String table, String fk, long id, long product, int qty, int price) throws Exception {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO " + table + "(" + fk + ",san_pham_id,so_luong,don_gia,thanh_tien,ghi_chu,ngay_tao,ngay_cap_nhat,version" +
                (table.equals("chi_tiet_phieu_nhap") ? ",so_luong_thuc_nhan,trang_thai_dong" : "") + ") VALUES(?,?,?,?,?,?,?,?,0" +
                (table.equals("chi_tiet_phieu_nhap") ? ",?,'KHOP'" : "") + ")")) {
            ps.setLong(1, id); ps.setLong(2, product); ps.setInt(3, qty); ps.setBigDecimal(4, BigDecimal.valueOf(price));
            ps.setBigDecimal(5, BigDecimal.valueOf((long) qty * price)); ps.setString(6, "Final demo line");
            ps.setTimestamp(7, ts(NOW)); ps.setTimestamp(8, ts(NOW));
            if (table.equals("chi_tiet_phieu_nhap")) ps.setInt(9, qty);
            ps.executeUpdate();
        }
    }

    private static void hist(Connection c, String table, String fk, long id, long actor, String action, String note) throws Exception {
        exec(c, "INSERT INTO " + table + "(" + fk + ",nguoi_thuc_hien_id,hanh_dong,ghi_chu,ngay_thuc_hien) VALUES (" +
                id + "," + actor + ",'" + action + "','" + note + "','" + NOW + "')");
    }

    private static void count(Connection c, String code, Inv r, Integer actual, Map<String, Long> products,
                              Map<String, Long> warehouses, long actor, String status) throws Exception {
        long id;
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO dot_kiem_ke(ma_dot,kho_id,trang_thai,ghi_chu,nguoi_tao_id,nguoi_chot_id,ngay_chot,ngay_tao,ngay_cap_nhat,version)
                VALUES(?,?,?,?::varchar,?,?,?,?,?,0) RETURNING id
                """)) {
            ps.setString(1, code); ps.setLong(2, warehouses.get(r.warehouse)); ps.setString(3, status); ps.setString(4, "Final demo count");
            ps.setLong(5, actor); setLongOrNull(ps, 6, status.equals("DA_CHOT") ? actor : null);
            ps.setTimestamp(7, status.equals("DA_CHOT") ? ts(NOW) : null); ps.setTimestamp(8, ts(NOW)); ps.setTimestamp(9, ts(NOW));
            try (ResultSet rs = ps.executeQuery()) { rs.next(); id = rs.getLong(1); }
        }
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO chi_tiet_kiem_ke(dot_kiem_ke_id,san_pham_id,so_luong_he_thong,so_luong_thuc_te,chenh_lech,ghi_chu,version)
                VALUES(?,?,?,?,?,?,0)
                """)) {
            ps.setLong(1, id); ps.setLong(2, products.get(r.product)); ps.setInt(3, r.qty);
            if (actual == null) { ps.setNull(4, java.sql.Types.INTEGER); ps.setNull(5, java.sql.Types.INTEGER); }
            else { ps.setInt(4, actual); ps.setInt(5, actual - r.qty); }
            ps.setString(6, "Final demo count detail"); ps.executeUpdate();
        }
    }

    private static List<Inv> readInventory() throws Exception {
        List<Inv> out = new ArrayList<>();
        for (String[] v : csv(OUT.resolve("demo-inventory.csv"))) out.add(new Inv(v[0], v[1], Integer.parseInt(v[2]), Integer.parseInt(v[3]), v[4], new BigDecimal(v[5])));
        return out;
    }
    private static List<Model> readModels() throws Exception {
        List<Model> out = new ArrayList<>();
        for (String[] v : csv(OUT.resolve("forecast-models.csv"))) out.add(new Model(v[0], v[1], v[2], Integer.parseInt(v[3]), new BigDecimal(v[4]), new BigDecimal(v[5]), new BigDecimal(v[6]), Integer.parseInt(v[7]), v[8], v[9], LocalDate.parse(v[10]), LocalDate.parse(v[11]), LocalDateTime.parse(v[12].replace(' ', 'T'))));
        return out;
    }
    private static List<Forecast> readForecasts() throws Exception {
        List<Forecast> out = new ArrayList<>();
        for (String[] v : csv(OUT.resolve("forecast-results.csv"))) out.add(new Forecast(v[0], Integer.parseInt(v[1]), LocalDate.parse(v[2]), new BigDecimal(v[3])));
        return out;
    }
    private static List<Daily> readDaily() throws Exception {
        List<Daily> out = new ArrayList<>();
        for (String[] v : csv(OUT.resolve("daily-forecast.csv"))) out.add(new Daily(v[0], LocalDate.parse(v[1]), new BigDecimal(v[2])));
        return out;
    }
    private static List<String[]> csv(Path path) throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(path)) {
            r.readLine();
            for (String line; (line = r.readLine()) != null; ) rows.add(line.split(",", -1));
        }
        return rows;
    }

    private static Map<String, Long> ids(Connection c, String table, String codeCol) throws Exception {
        Map<String, Long> out = new HashMap<>();
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT id," + codeCol + " FROM " + table)) {
            while (rs.next()) out.put(rs.getString(2), rs.getLong(1));
        }
        return out;
    }
    private static long employee(Connection c, String... roles) throws Exception {
        String in = "'" + String.join("','", roles) + "'";
        Long id = scalarLong(c, "SELECT nv.id FROM nhan_vien nv JOIN vai_tro vt ON vt.id=nv.vai_tro_id WHERE vt.ma_vai_tro IN (" + in + ") AND nv.trang_thai='HOAT_DONG' ORDER BY nv.id LIMIT 1");
        if (id == null) throw new IllegalStateException("missing employee role " + in);
        return id;
    }
    private static Long scalarLong(Connection c, String sql) throws Exception {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : null;
        }
    }
    private static void assertCount(Connection c, String sql, long expected) throws Exception {
        Long actual = scalarLong(c, sql);
        if (actual == null || actual != expected) throw new IllegalStateException(sql + " expected " + expected + " got " + actual);
    }
    private static void exec(Connection c, String sql) throws Exception {
        try (Statement s = c.createStatement()) { s.executeUpdate(sql); }
    }
    private static void execIfExists(Connection c, String table, String sql) throws Exception {
        if (scalarLong(c, "SELECT CASE WHEN to_regclass('" + table + "') IS NULL THEN 0 ELSE 1 END") == 1) exec(c, sql);
    }
    private static void checkIds(List<Inv> rows, Map<String, Long> products, Map<String, Long> warehouses) {
        for (Inv r : rows) {
            if (!products.containsKey(r.product)) throw new IllegalStateException("missing product " + r.product);
            if (!warehouses.containsKey(r.warehouse)) throw new IllegalStateException("missing warehouse " + r.warehouse);
        }
    }
    private static Timestamp ts(LocalDateTime time) { return Timestamp.valueOf(time); }
    private static void setLongOrNull(PreparedStatement ps, int i, Long value) throws Exception {
        if (value == null) ps.setNull(i, java.sql.Types.BIGINT); else ps.setLong(i, value);
    }
    private static void requireEnv(String name) {
        if (System.getenv(name) == null || System.getenv(name).isBlank()) throw new IllegalStateException(name + " is required");
    }
}
