package com.smartflow.smestocksensebackend.seeder;

import com.smartflow.smestocksensebackend.entity.Category;
import com.smartflow.smestocksensebackend.entity.CategoryStatus;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.PartnerStatus;
import com.smartflow.smestocksensebackend.entity.PartnerType;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.repository.CategoryRepository;
import com.smartflow.smestocksensebackend.repository.PartnerRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seed dữ liệu sản phẩm mẫu (kèm danh mục, nhà cung cấp) để dễ test UI.
 * Chạy sau UserSeeder. Idempotent: chỉ tạo khi chưa tồn tại theo mã.
 */
@Component
@Order(2)
public class ProductSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PartnerRepository partnerRepository;

    public ProductSeeder(ProductRepository productRepository,
                         CategoryRepository categoryRepository,
                         PartnerRepository partnerRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.partnerRepository = partnerRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("🔄 [ProductSeeder] Đang kiểm tra và seed dữ liệu sản phẩm mẫu...");

        Category luongThuc = seedCategory("DM-LT", "Lương thực");
        Category doUong = seedCategory("DM-DU", "Đồ uống");

        Partner ncc = seedSupplier("NCC-001", "Công ty TNHH Thực phẩm Miền Nam");

        seedProduct("SP-GAO", "Gạo ST25 túi 5kg", "SKU-GAO-5KG", "8930001112223",
                "Túi", new BigDecimal("145000"), luongThuc, ncc, 20);
        seedProduct("SP-CAFE", "Cà phê rang xay 500g", "SKU-CAFE-500", "8930004445556",
                "Gói", new BigDecimal("95000"), doUong, ncc, 15);
        seedProduct("SP-DUONG", "Đường tinh luyện 1kg", "SKU-DUONG-1KG", "8930007778889",
                "Túi", new BigDecimal("23000"), luongThuc, ncc, 30);

        System.out.println("✅ [ProductSeeder] Đã hoàn thành seed dữ liệu sản phẩm mẫu.");
    }

    private Category seedCategory(String code, String name) {
        return categoryRepository.findAll().stream()
                .filter(c -> code.equalsIgnoreCase(c.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setCode(code);
                    category.setName(name);
                    category.setStatus(CategoryStatus.HOAT_DONG);
                    Category saved = categoryRepository.save(category);
                    System.out.println("   -> Đã tạo danh mục: " + name);
                    return saved;
                });
    }

    private Partner seedSupplier(String code, String name) {
        if (partnerRepository.existsByCodeIgnoreCase(code)) {
            return partnerRepository.findAll().stream()
                    .filter(p -> code.equalsIgnoreCase(p.getCode()))
                    .findFirst()
                    .orElseThrow();
        }
        Partner partner = new Partner();
        partner.setCode(code);
        partner.setName(name);
        partner.setType(PartnerType.NHA_CUNG_CAP);
        partner.setStatus(PartnerStatus.HOAT_DONG);
        Partner saved = partnerRepository.save(partner);
        System.out.println("   -> Đã tạo nhà cung cấp: " + name);
        return saved;
    }

    private void seedProduct(String code, String name, String sku, String barcode,
                            String unit, BigDecimal price, Category category, Partner partner, int minStock) {
        if (productRepository.existsByCodeIgnoreCase(code)) {
            return;
        }
        Product product = new Product();
        product.setCode(code);
        product.setName(name);
        product.setSku(sku);
        product.setBarcode(barcode);
        product.setUnit(unit);
        product.setPrice(price);
        product.setCategory(category);
        product.setPartner(partner);
        product.setMinStock(minStock);
        product.setStatus(ProductStatus.HOAT_DONG);
        productRepository.save(product);
        System.out.println("   -> Đã tạo sản phẩm: " + name);
    }
}
