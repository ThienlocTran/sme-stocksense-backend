package com.smartflow.smestocksensebackend.externalretail;

import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalRetailImporterTest {

    @TempDir
    Path tempDir;

    @Test
    void loadMapping_parsesStoresAndItems() throws Exception {
        Path mapping = mapping("i01,i02");
        ExternalRetailImporter importer = importer();

        ExternalRetailImporter.Mapping result = importer.loadMapping(mapping);

        assertThat(result.stores()).containsKeys("1", "2", "3");
        assertThat(result.items()).containsKeys("i01", "i02");
    }

    @Test
    void buildPlan_aggregatesContinuityZeroFillAndPrice() throws Exception {
        Path sales = sales("""
                ,date,item_id,quantity,price_base,sum_total,store_id
                0,2024-01-01,i01,2,10,20,1
                1,2024-01-01,i01,3,10,30,1
                2,2024-01-02,i01,0,10,0,1
                3,2024-01-01,i02,-1,10,10,1
                4,2024-01-01,i01,1,-5,5,1
                5,2024-01-01,i03,5,10,50,1
                6,2024-01-01,i01,1,10,10,2
                7,2024-01-01,i01,1,10,10,3
                """);
        Path mapping = mapping("i01,i03");
        ExternalRetailImporter importer = importer();

        ExternalRetailImporter.Plan plan = importer.buildPlan(sales, importer.loadMapping(mapping),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2));

        assertThat(plan.stats().negativeQuantityRows).isEqualTo(1);
        assertThat(plan.stats().invalidPriceRows).isEqualTo(1);
        assertThat(plan.stats().zeroFilledDays).isEqualTo(7);
        assertThat(plan.rows()).hasSize(12);
        assertThat(plan.rows()).anySatisfy(row -> {
            assertThat(row.productCode()).isEqualTo("SP001");
            assertThat(row.warehouseCode()).isEqualTo("K001");
            assertThat(row.date()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(row.quantity()).isEqualTo(6);
            assertThat(row.averagePrice()).isEqualByComparingTo(new BigDecimal("10.00"));
            assertThat(row.sourceReference()).isEqualTo("EXTERNAL_RETAIL:1:i01");
        });
    }

    @Test
    void buildPlan_failsWhenMappingOrderDiffersFromDeterministicSelection() throws Exception {
        Path sales = sales("""
                ,date,item_id,quantity,price_base,sum_total,store_id
                0,2024-01-01,i01,10,10,100,1
                1,2024-01-01,i02,1,10,10,1
                2,2024-01-01,i01,1,10,10,2
                3,2024-01-01,i01,1,10,10,3
                """);
        Path mapping = mapping("i02,i01");
        ExternalRetailImporter importer = importer();

        assertThatThrownBy(() -> importer.buildPlan(sales, importer.loadMapping(mapping),
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)))
                .hasMessageContaining("ITEM mapping does not match deterministic selection");
    }

    @Test
    void resolveTargets_usesBusinessCodesAndFailsWhenMissing() throws Exception {
        ProductRepository products = mock(ProductRepository.class);
        WarehouseRepository warehouses = mock(WarehouseRepository.class);
        Product product = new Product();
        product.setId(10L);
        Warehouse warehouse = new Warehouse();
        warehouse.setId(20L);
        when(products.findByCodeIgnoreCase("SP001")).thenReturn(Optional.of(product));
        when(warehouses.findByCodeIgnoreCase("K001")).thenReturn(Optional.of(warehouse));
        when(warehouses.findByCodeIgnoreCase("K002")).thenReturn(Optional.of(warehouse));
        when(warehouses.findByCodeIgnoreCase("K003")).thenReturn(Optional.of(warehouse));
        ExternalRetailImporter importer = new ExternalRetailImporter(products, warehouses, mock(JdbcTemplate.class));

        ExternalRetailImporter.Resolution resolution = importer.resolveTargets(importer.loadMapping(mapping("i01")));

        assertThat(resolution.productIds()).containsEntry("SP001", 10L);
        assertThat(resolution.warehouseIds()).containsEntry("K001", 20L);
    }

    @Test
    void resolveTargets_failsForMissingMappedProduct() throws Exception {
        WarehouseRepository warehouses = mock(WarehouseRepository.class);
        Warehouse warehouse = new Warehouse();
        warehouse.setId(20L);
        when(warehouses.findByCodeIgnoreCase("K001")).thenReturn(Optional.of(warehouse));
        when(warehouses.findByCodeIgnoreCase("K002")).thenReturn(Optional.of(warehouse));
        when(warehouses.findByCodeIgnoreCase("K003")).thenReturn(Optional.of(warehouse));
        ExternalRetailImporter importer = new ExternalRetailImporter(mock(ProductRepository.class), warehouses,
                mock(JdbcTemplate.class));

        assertThatThrownBy(() -> importer.resolveTargets(importer.loadMapping(mapping("i01"))))
                .hasMessageContaining("Missing product: SP001");
    }

    private ExternalRetailImporter importer() {
        return new ExternalRetailImporter(mock(ProductRepository.class), mock(WarehouseRepository.class),
                mock(JdbcTemplate.class));
    }

    private Path sales(String content) throws Exception {
        Path source = tempDir.resolve("sales.csv");
        Files.writeString(source, content);
        return source;
    }

    private Path mapping(String itemCsv) throws Exception {
        Path mapping = tempDir.resolve("mapping.csv");
        StringBuilder csv = new StringBuilder("""
                type,external_id,stocksense_code,rank,metadata
                STORE,1,K001,1,test
                STORE,2,K002,2,test
                STORE,3,K003,3,test
                """);
        String[] items = itemCsv.split(",");
        for (int i = 0; i < items.length; i++) {
            csv.append("ITEM,").append(items[i]).append(",SP")
                    .append(String.format("%03d", i + 1)).append(',').append(i + 1).append(",test\n");
        }
        Files.writeString(mapping, csv.toString());
        return mapping;
    }
}
