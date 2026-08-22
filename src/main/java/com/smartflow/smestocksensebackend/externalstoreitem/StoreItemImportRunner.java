package com.smartflow.smestocksensebackend.externalstoreitem;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;

@Component
public class StoreItemImportRunner implements ApplicationRunner {

    private final StoreItemImporter importer;
    private final ConfigurableApplicationContext context;

    public StoreItemImportRunner(StoreItemImporter importer, ConfigurableApplicationContext context) {
        this.importer = importer;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("store-item-import")) {
            return;
        }
        Path source = Path.of(value(args, "source",
                "D:/SME-stock-sense/data/data_train/retail_sales.csv"));
        Path mapping = Path.of(value(args, "mapping",
                "src/main/resources/ai/store-item-mapping-v1.csv"));
        LocalDate start = LocalDate.parse(value(args, "start-date", StoreItemImporter.DEFAULT_START.toString()));
        LocalDate end = LocalDate.parse(value(args, "end-date", StoreItemImporter.DEFAULT_END.toString()));
        boolean dryRun = Boolean.parseBoolean(value(args, "dry-run", "true"));
        int batchSize = Integer.parseInt(value(args, "batch-size", "1000"));
        StoreItemImporter.ImportResult result = importer.run(
                new StoreItemImporter.ImportOptions(source, mapping, start, end, dryRun, batchSize));
        System.out.println(result.toReport());
        context.close();
    }

    private String value(ApplicationArguments args, String name, String defaultValue) {
        return args.containsOption(name) && !args.getOptionValues(name).isEmpty()
                ? args.getOptionValues(name).get(0)
                : defaultValue;
    }
}
