package com.smartflow.smestocksensebackend.externalretail;

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
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ExternalRetailImporter {

    static final LocalDate DEFAULT_START = LocalDate.of(2023, 10, 1);
    static final LocalDate DEFAULT_END = LocalDate.of(2024, 9, 26);
    static final String SOURCE = SalesHistorySource.EXTERNAL_RETAIL.name();

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final JdbcTemplate jdbcTemplate;

    public ExternalRetailImporter(ProductRepository productRepository, WarehouseRepository warehouseRepository,
            JdbcTemplate jdbcTemplate) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ImportResult run(ImportOptions options) throws IOException {
        Mapping mapping = loadMapping(options.mappingPath());
        Plan plan = buildPlan(options.sourcePath().resolve("sales.csv"), mapping, options.startDate(), options.endDate());
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
        return new Mapping(stores, items);
    }

    Plan buildPlan(Path salesPath, Mapping mapping, LocalDate start, LocalDate end) throws IOException {
        List<String> selectedStores = selectStores(salesPath);
        List<String> selectedItems = selectItems(salesPath, start, end);
        requireOrder("STORE", selectedStores, new ArrayList<>(mapping.stores().keySet()));
        requireOrder("ITEM", selectedItems, new ArrayList<>(mapping.items().keySet()));

        Map<Key, Amount> aggregated = new HashMap<>();
        Stats stats = new Stats(selectedStores, selectedItems, start, end);
        try (BufferedReader reader = Files.newBufferedReader(salesPath, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            Map<String, Integer> columns = columns(header);
            String line;
            while ((line = reader.readLine()) != null) {
                stats.sourceRows++;
                List<String> row = csv(line);
                ParsedSale sale = parse(row, columns);
                if (sale == null || sale.quantity() == null) {
                    stats.rejectedRows++;
                    continue;
                }
                if (sale.quantity().signum() < 0) {
                    stats.rejectedRows++;
                    stats.negativeQuantityRows++;
                    continue;
                }
                stats.validRows++;
                if (sale.quantity().signum() == 0) {
                    stats.zeroQuantityRows++;
                }
                if (!mapping.stores().containsKey(sale.storeId()) || !mapping.items().containsKey(sale.itemId())
                        || sale.date().isBefore(start) || sale.date().isAfter(end)) {
                    continue;
                }
                stats.subsetRows++;
                Key key = new Key(sale.storeId(), sale.itemId(), sale.date());
                Amount amount = aggregated.computeIfAbsent(key, ignored -> new Amount());
                amount.quantity = amount.quantity.add(sale.quantity());
                if (sale.quantity().signum() > 0 && !sale.badPrice()) {
                    amount.total = amount.total.add(sale.total());
                    amount.priceQuantity = amount.priceQuantity.add(sale.quantity());
                } else if (sale.badPrice()) {
                    stats.invalidPriceRows++;
                }
            }
        }

        List<ImportRow> rows = new ArrayList<>();
        for (String storeId : mapping.stores().keySet()) {
            for (String itemId : mapping.items().keySet()) {
                for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
                    Amount amount = aggregated.get(new Key(storeId, itemId, day));
                    BigDecimal quantity = amount == null ? BigDecimal.ZERO : amount.quantity;
                    BigDecimal price = amount == null || amount.priceQuantity.signum() <= 0 || amount.total.signum() <= 0
                            ? null
                            : amount.total.divide(amount.priceQuantity, 2, RoundingMode.HALF_UP);
                    if (amount == null) {
                        stats.zeroFilledDays++;
                    }
                    if (price != null) {
                        stats.priceRows++;
                    }
                    rows.add(new ImportRow(mapping.items().get(itemId).stockSenseCode(),
                            mapping.stores().get(storeId).stockSenseCode(), day, quantity.intValue(), price,
                            SOURCE + ":" + storeId + ":" + itemId));
                }
            }
        }
        stats.mappedRows = aggregated.size();
        stats.finalRows = rows.size();
        stats.duplicatesAggregated = Math.max(0, stats.subsetRows - aggregated.size());
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
                    (san_pham_id, kho_id, ngay, so_luong, gia_ban_binh_quan, nguon_du_lieu, tham_chieu_nguon, ngay_tao, ngay_cap_nhat)
                VALUES (?, ?, ?, ?, ?, ?, ?, now(), now())
                ON CONFLICT (san_pham_id, kho_id, ngay, nguon_du_lieu)
                DO UPDATE SET so_luong = EXCLUDED.so_luong,
                    gia_ban_binh_quan = EXCLUDED.gia_ban_binh_quan,
                    tham_chieu_nguon = EXCLUDED.tham_chieu_nguon,
                    ngay_cap_nhat = now()
                """;
        jdbcTemplate.batchUpdate(sql, rows, batchSize, new RowSetter(resolution));
    }

    private List<String> selectStores(Path salesPath) throws IOException {
        Set<String> stores = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(salesPath, StandardCharsets.UTF_8)) {
            Map<String, Integer> columns = columns(reader.readLine());
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> row = csv(line);
                stores.add(cell(row, columns, "store_id"));
            }
        }
        return stores.stream().sorted(Comparator.comparingInt(Integer::parseInt)).limit(3).toList();
    }

    private List<String> selectItems(Path salesPath, LocalDate start, LocalDate end) throws IOException {
        Map<String, BigDecimal> totals = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(salesPath, StandardCharsets.UTF_8)) {
            Map<String, Integer> columns = columns(reader.readLine());
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> row = csv(line);
                ParsedSale sale = parse(row, columns);
                if (sale != null && sale.quantity() != null && sale.quantity().signum() > 0
                        && !sale.date().isBefore(start) && !sale.date().isAfter(end)) {
                    totals.merge(sale.itemId(), sale.quantity(), BigDecimal::add);
                }
            }
        }
        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(12)
                .map(Map.Entry::getKey)
                .toList();
    }

    private ParsedSale parse(List<String> row, Map<String, Integer> columns) {
        try {
            LocalDate date = LocalDate.parse(cell(row, columns, "date"));
            String itemId = cell(row, columns, "item_id");
            String storeId = cell(row, columns, "store_id");
            if (itemId.isBlank() || storeId.isBlank()) {
                return null;
            }
            BigDecimal quantity = decimal(cell(row, columns, "quantity")).orElse(null);
            Optional<BigDecimal> total = decimal(cell(row, columns, "sum_total"));
            boolean badPrice = total.isEmpty() || total.get().signum() < 0
                    || decimal(cell(row, columns, "price_base")).filter(v -> v.signum() >= 0).isEmpty();
            return new ParsedSale(date, itemId, storeId, quantity, total.orElse(null), badPrice);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Optional<BigDecimal> decimal(String text) {
        try {
            return Optional.of(new BigDecimal(text));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private Map<String, Integer> columns(String header) {
        List<String> names = csv(header);
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            columns.put(names.get(i), i);
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

    private void requireOrder(String type, List<String> selected, List<String> mapped) {
        if (!selected.equals(mapped)) {
            throw new IllegalStateException(type + " mapping does not match deterministic selection. selected="
                    + selected + ", mapped=" + mapped);
        }
    }

    public record ImportOptions(Path sourcePath, Path mappingPath, LocalDate startDate, LocalDate endDate,
            boolean dryRun, int batchSize) {
    }

    public record ImportResult(Stats stats, Resolution resolution, int insertedRows) {
        public String toReport() {
            long days = ChronoUnit.DAYS.between(stats.startDate, stats.endDate) + 1;
            return """
                    EXTERNAL_RETAIL DRY RUN=%s
                    source rows=%d
                    valid rows=%d
                    rejected rows=%d
                    negative quantity rejected=%d
                    zero quantity source rows=%d
                    invalid price rows=%d
                    duplicates aggregated=%d
                    mapped rows=%d
                    selected stores=%s
                    selected items=%s
                    series count=%d
                    calendar days=%d
                    zero-filled days=%d
                    final rows=%d
                    price coverage=%d/%d
                    estimated write count=%d
                    resolved products=%s
                    resolved warehouses=%s
                    inserted rows=%d
                    """.formatted(insertedRows == 0, stats.sourceRows, stats.validRows, stats.rejectedRows,
                    stats.negativeQuantityRows, stats.zeroQuantityRows, stats.invalidPriceRows,
                    stats.duplicatesAggregated, stats.mappedRows, stats.selectedStores, stats.selectedItems,
                    stats.selectedStores.size() * stats.selectedItems.size(), days, stats.zeroFilledDays,
                    stats.finalRows, stats.priceRows, stats.finalRows, stats.finalRows,
                    resolution.productIds().keySet(), resolution.warehouseIds().keySet(), insertedRows);
        }
    }

    public static class Stats {
        public final List<String> selectedStores;
        public final List<String> selectedItems;
        public final LocalDate startDate;
        public final LocalDate endDate;
        public long sourceRows;
        public long validRows;
        public long rejectedRows;
        public long negativeQuantityRows;
        public long zeroQuantityRows;
        public long invalidPriceRows;
        public long subsetRows;
        public long mappedRows;
        public long duplicatesAggregated;
        public long zeroFilledDays;
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

    record ParsedSale(LocalDate date, String itemId, String storeId, BigDecimal quantity, BigDecimal total,
            boolean badPrice) {
    }

    static class Amount {
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal priceQuantity = BigDecimal.ZERO;
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
            ps.setBigDecimal(5, row.averagePrice());
            ps.setString(6, SOURCE);
            ps.setString(7, row.sourceReference());
        }
    }
}
