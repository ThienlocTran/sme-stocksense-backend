package com.smartflow.smestocksensebackend.externalstoreitem;

import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StoreItemImporter {

    static final LocalDate DEFAULT_START = LocalDate.of(2019, 1, 1);
    static final LocalDate DEFAULT_END = LocalDate.of(2023, 12, 31);
    static final String SOURCE = SalesHistorySource.EXTERNAL_STORE_ITEM.name();

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final JdbcTemplate jdbcTemplate;

    public StoreItemImporter(ProductRepository productRepository, WarehouseRepository warehouseRepository,
            JdbcTemplate jdbcTemplate) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ImportResult run(ImportOptions options) throws IOException {
        Mapping mapping = loadMapping(options.mappingPath());
        Plan plan = buildPlan(options.sourcePath(), mapping, options.startDate(), options.endDate());
        Resolution resolution = resolveTargets(mapping);
        if (!options.dryRun()) {
            upsert(plan.rows(), resolution, options.batchSize());
        }
        return new ImportResult(plan.stats(), resolution, options.dryRun() ? 0 : plan.rows().size());
    }

    Mapping loadMapping(Path path) throws IOException {
        Map<String, MappingEntry> stores = new LinkedHashMap<>();
        Map<String, MappingEntry> items = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null || !header.startsWith("type,external_id,stocksense_code,rank")) {
                throw new IllegalArgumentException("Mapping header invalid: " + path);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> cells = csv(line);
                MappingEntry entry = new MappingEntry(cells.get(0), cells.get(1), cells.get(2),
                        Integer.parseInt(cells.get(3)));
                if ("STORE".equals(entry.type())) {
                    stores.put(entry.externalId(), entry);
                } else if ("ITEM".equals(entry.type())) {
                    items.put(entry.externalId(), entry);
                }
            }
        }
        if (stores.size() != 3 || items.size() != 50) {
            throw new IllegalStateException("Store-Item mapping must contain 3 stores and 50 items.");
        }
        return new Mapping(stores, items);
    }

    Plan buildPlan(Path salesPath, Mapping mapping, LocalDate start, LocalDate end) throws IOException {
        List<ImportRow> rows = new ArrayList<>();
        Stats stats = new Stats(new ArrayList<>(mapping.stores().keySet()), new ArrayList<>(mapping.items().keySet()),
                start, end);
        Map<Key, Integer> seen = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(salesPath, StandardCharsets.UTF_8)) {
            Map<String, Integer> columns = columns(reader.readLine());
            String line;
            while ((line = reader.readLine()) != null) {
                stats.sourceRows++;
                List<String> row = csv(line);
                ParsedSale sale = parse(row, columns);
                if (sale == null) {
                    stats.rejectedRows++;
                    continue;
                }
                if (!mapping.items().containsKey(sale.itemId()) || !mapping.stores().containsKey(sale.storeId())) {
                    continue;
                }
                if (sale.date().isBefore(start) || sale.date().isAfter(end)) {
                    continue;
                }
                stats.subsetRows++;
                if (sale.quantity() < 0) {
                    stats.negativeQuantityRows++;
                    throw new IllegalStateException("Negative sales at " + sale);
                }
                Key key = new Key(sale.storeId(), sale.itemId(), sale.date());
                int count = seen.merge(key, 1, Integer::sum);
                if (count > 1) {
                    stats.duplicates++;
                    throw new IllegalStateException("Duplicate item/store/date: " + key);
                }
                if (sale.quantity() == 0) {
                    stats.zeroQuantityRows++;
                }
                if (sale.price() != null) {
                    stats.priceRows++;
                }
                rows.add(new ImportRow(mapping.items().get(sale.itemId()).stockSenseCode(),
                        mapping.stores().get(sale.storeId()).stockSenseCode(), sale.date(), sale.quantity(),
                        sale.price(), SOURCE + ":" + sale.itemId() + ":" + sale.storeId()));
            }
        }
        stats.finalRows = rows.size();
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        long expectedRows = (long) mapping.items().size() * mapping.stores().size() * days;
        if (stats.finalRows != expectedRows) {
            throw new IllegalStateException("Expected " + expectedRows + " rows, got " + stats.finalRows);
        }
        return new Plan(rows, stats);
    }

    Resolution resolveTargets(Mapping mapping) {
        Map<String, Long> products = new HashMap<>();
        for (MappingEntry entry : mapping.items().values()) {
            Product product = productRepository.findByCodeIgnoreCase(entry.stockSenseCode())
                    .orElseThrow(() -> new IllegalStateException("Missing product: " + entry.stockSenseCode()));
            products.put(entry.stockSenseCode(), product.getId());
        }
        Map<String, Long> warehouses = new HashMap<>();
        for (MappingEntry entry : mapping.stores().values()) {
            Warehouse warehouse = warehouseRepository.findByCodeIgnoreCase(entry.stockSenseCode())
                    .orElseThrow(() -> new IllegalStateException("Missing warehouse: " + entry.stockSenseCode()));
            warehouses.put(entry.stockSenseCode(), warehouse.getId());
        }
        return new Resolution(products, warehouses);
    }

    private void upsert(List<ImportRow> rows, Resolution resolution, int batchSize) {
        String sql = """
                INSERT INTO ai.lich_su_ban_hang
                    (san_pham_id, kho_id, ngay, so_luong, nguon, gia_ban_binh_quan, nguon_du_lieu, tham_chieu_nguon, ngay_tao, ngay_cap_nhat)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                ON CONFLICT (san_pham_id, kho_id, ngay, nguon_du_lieu)
                DO UPDATE SET so_luong = EXCLUDED.so_luong,
                    gia_ban_binh_quan = EXCLUDED.gia_ban_binh_quan,
                    tham_chieu_nguon = EXCLUDED.tham_chieu_nguon,
                    ngay_cap_nhat = now()
                """;
        jdbcTemplate.batchUpdate(sql, rows, batchSize, new RowSetter(resolution));
    }

    private ParsedSale parse(List<String> row, Map<String, Integer> columns) {
        try {
            LocalDate date = LocalDate.parse(cell(row, columns, "date"));
            String itemId = cell(row, columns, "item_id");
            String storeId = cell(row, columns, "store_id");
            int quantity = Integer.parseInt(cell(row, columns, "sales"));
            BigDecimal price = new BigDecimal(cell(row, columns, "price"));
            return new ParsedSale(date, itemId, storeId, quantity, price);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Map<String, Integer> columns(String header) {
        List<String> names = csv(header);
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            columns.put(names.get(i), i);
        }
        for (String required : List.of("date", "store_id", "item_id", "sales", "price")) {
            if (!columns.containsKey(required)) {
                throw new IllegalArgumentException("Missing column: " + required);
            }
        }
        return columns;
    }

    private String cell(List<String> row, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        return index == null || index >= row.size() ? "" : row.get(index);
    }

    private List<String> csv(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                quoted = !quoted;
            } else if (ch == ',' && !quoted) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(ch);
            }
        }
        cells.add(cell.toString());
        return cells;
    }

    public record ImportOptions(Path sourcePath, Path mappingPath, LocalDate startDate, LocalDate endDate,
            boolean dryRun, int batchSize) {
    }

    public record ImportResult(Stats stats, Resolution resolution, int affectedRows) {
        public String toReport() {
            long days = ChronoUnit.DAYS.between(stats.startDate, stats.endDate) + 1;
            return """
                    EXTERNAL_STORE_ITEM DRY RUN=%s
                    source rows=%d
                    subset rows=%d
                    rejected rows=%d
                    negative quantity rows=%d
                    zero quantity source rows=%d
                    duplicate item/store/date rows=%d
                    selected stores=%s
                    selected items=%s
                    series count=%d
                    calendar days=%d
                    final rows=%d
                    price coverage=%d/%d
                    resolved products=%s
                    resolved warehouses=%s
                    affected rows=%d
                    """.formatted(affectedRows == 0, stats.sourceRows, stats.subsetRows, stats.rejectedRows,
                    stats.negativeQuantityRows, stats.zeroQuantityRows, stats.duplicates,
                    stats.selectedStores, stats.selectedItems, stats.selectedStores.size() * stats.selectedItems.size(),
                    days, stats.finalRows, stats.priceRows, stats.finalRows, resolution.productIds().keySet(),
                    resolution.warehouseIds().keySet(), affectedRows);
        }
    }

    public static class Stats {
        public final List<String> selectedStores;
        public final List<String> selectedItems;
        public final LocalDate startDate;
        public final LocalDate endDate;
        public long sourceRows;
        public long subsetRows;
        public long rejectedRows;
        public long negativeQuantityRows;
        public long zeroQuantityRows;
        public long duplicates;
        public long finalRows;
        public long priceRows;

        Stats(List<String> selectedStores, List<String> selectedItems, LocalDate startDate, LocalDate endDate) {
            this.selectedStores = selectedStores;
            this.selectedItems = selectedItems;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    public record Mapping(Map<String, MappingEntry> stores, Map<String, MappingEntry> items) {
    }

    public record MappingEntry(String type, String externalId, String stockSenseCode, int rank) {
    }

    public record Resolution(Map<String, Long> productIds, Map<String, Long> warehouseIds) {
    }

    public record ImportRow(String productCode, String warehouseCode, LocalDate date, int quantity,
            BigDecimal averagePrice, String sourceReference) {
    }

    record Plan(List<ImportRow> rows, Stats stats) {
    }

    record Key(String storeId, String itemId, LocalDate date) {
    }

    record ParsedSale(LocalDate date, String itemId, String storeId, int quantity, BigDecimal price) {
    }

    static class RowSetter implements ParameterizedPreparedStatementSetter<ImportRow> {
        private final Resolution resolution;

        RowSetter(Resolution resolution) {
            this.resolution = resolution;
        }

        @Override
        public void setValues(PreparedStatement ps, ImportRow row) throws SQLException {
            ps.setLong(1, resolution.productIds().get(row.productCode()));
            ps.setLong(2, resolution.warehouseIds().get(row.warehouseCode()));
            ps.setDate(3, Date.valueOf(row.date()));
            ps.setInt(4, row.quantity());
            ps.setString(5, "EXTERNAL");
            ps.setBigDecimal(6, row.averagePrice());
            ps.setString(7, SOURCE);
            ps.setString(8, row.sourceReference());
        }
    }
}
